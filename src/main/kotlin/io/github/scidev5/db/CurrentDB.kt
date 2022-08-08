package io.github.scidev5.db

import io.github.scidev5.commandLine.*
import io.github.scidev5.commandLine.DefaultConfirmation
import io.github.scidev5.workingDir
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*


class CurrentDB {
    private enum class DBCommand(val label:String) {
        HELP      ("help"),
        CLOSE     ("close"),
        LIST_OPEN ("list opened"),
        SAVE      ("save"),
        VIEW      ("view"),
        CREATE    ("create"),
        DELETE    ("delete"),
        DELETE_DB ("delete database"),
        PAUSE     ("pause"),
        PRINT     ("print");

        companion object {
            fun parse(string: String): DBCommand? {
                return all.firstOrNull { it.label == string }
                    ?: when (string) {
                        "h", "?"    -> HELP
                        "x"         -> CLOSE
                        "s", "push" -> SAVE
                        "v", "pull" -> VIEW
                        "c"         -> CREATE
                        "del"       -> DELETE
                        "l", "list" -> LIST_OPEN
                        "..."       -> PAUSE
                        "!"         -> PRINT
                        else -> null
                    }
            }
            val all = arrayOf(HELP,CLOSE,SAVE,VIEW,CREATE,DELETE,DELETE_DB,LIST_OPEN,PRINT,PAUSE)
        }
    }
    private class DBCommandParamPair(val command: DBCommand?, val params: List<String>)
    private enum class DBCommandSource {
        USER, CLI
    }

    companion object {
        fun init() {
            val config = DBConfig()

            config.remoteDir = Paths.get(AskForInput.lineOrNevermind(
                "enter the database storage folder path (right click to paste)\n",
                "valid path"
            ) {
                try {
                    Paths.get(it); true
                } catch (e: InvalidPathException) {
                    false
                }
            }
                ?: return println("cancelled")
            )

            config.save()

            val openShPath = workingDir / "open.sh"
            openShPath.writeText(DBInitData.openSHContent)
            try {
                openShPath.setPosixFilePermissions(
                    setOf(PosixFilePermission.OWNER_EXECUTE) + openShPath.getPosixFilePermissions()
                )
            } catch (_: UnsupportedOperationException) {}

            println("initialized coded-file-db at ${workingDir.absolutePathString()}")
        }

        fun run(commandListString: String) {
            if (!ProgramArguments.autoConfirmFlag)
                println("'-y' was not set, manual confirmation will be required at each command.")

            val commands = commandListString.split(Regex.fromLiteral(";")).map { it.trim() }

            run withDBOpen@{
                val db = CurrentDB()
                db.requireInitialized() ?: return@withDBOpen
                for (lineIn in commands) {
                    val input = parseCommand(lineIn)
                    db.runCommand(input, DBCommandSource.CLI)
                        ?: return@withDBOpen
                }
                db.close(allowAbort = false)
            }
        }

        fun open() {
            val db = CurrentDB()
            db.requireInitialized() ?: return

            loopWithDBOpen@ while (true) {
                val input = AskForInput.obj(
                    "enter command",
                    Companion::parseCommand
                )
                db.runCommand(input, DBCommandSource.USER)
                    ?: break@loopWithDBOpen
            }
        }

        private fun parseCommand(lineIn:String):DBCommandParamPair {
            val (rawCommand, rawParam) = lineIn.split(Regex(":"), 2) + listOf("")
            return DBCommandParamPair(
                DBCommand.parse(rawCommand.trim()),
                rawParam.split(Regex(",")).map { it.trim() }
            )
        }
    }
    private fun runCommand(input:DBCommandParamPair?, source: DBCommandSource):Unit? {
        return when (input?.command) {
            DBCommand.HELP -> Help.printDBOpen()

            DBCommand.CREATE -> execForeachParam(input.params, this::create)
            DBCommand.DELETE -> execForeachParam(input.params, this::delete)
            DBCommand.SAVE -> execForeachParam(input.params, this::push)
            DBCommand.VIEW -> execForeachParam(input.params, this::pull)

            DBCommand.LIST_OPEN -> listOpened()

            DBCommand.DELETE_DB -> deleteDatabase()

            null -> println("invalid command, try one of [${DBCommand.all.joinToString(", ") { "'${it.label}'" }}]")

            else -> when (source) {
                DBCommandSource.CLI -> when (input.command) {
                    DBCommand.PAUSE -> {
                        print("... paused, press enter to continue ...")
                        readln()
                        Unit
                    }
                    DBCommand.PRINT -> println(input.params.joinToString("\n"))
                    else -> println("disallowed command for CLI input: ${input.command.name}")
                }
                DBCommandSource.USER -> when (input.command) {
                    DBCommand.CLOSE -> close()
                    else -> println("disallowed command for user input: ${input.command.name}")
                }
            }
        }
    }

    private val config = DBConfig()
    fun requireInitialized():Unit? {
        return if (!config.initialized) {
            println(">> no config present in working directory.\n>> please navigate to a directory linked to a database.")
            null
        } else Unit
    }

    private val db: DBRemote = DBRemote(config.remoteDir)
    private val pulledFolders = HashMap<String, DBFolder>()
    private fun askForFolderName() = AskForInput.lineOrNevermind(
        "file group name?: ",
        "group name"
    ) { it.isNotBlank() }?.trim()
    private fun folderPath(folderName: String) = config.localDir / folderName
    private fun detectPulledFolder(folderName: String):Boolean {
        if (folderName in pulledFolders.keys)
            return true
        val folderPath = folderPath(folderName)
        return if (folderPath.isDirectory()) {
            pulledFolders[folderName] = DBFolder(folderPath)
            true
        } else false
    }
    private fun execForeachParam(paramsIn: List<String>, handle: (String?)->Unit) {
        val params = paramsIn.filter { it.isNotBlank() }
        if (params.isEmpty())  handle(null)
        else                   params.forEach(handle)
    }


    private fun close(allowAbort:Boolean = true):Unit? {
        if (pulledFolders.isNotEmpty()) {
            if (allowAbort)
                confirm("unsaved changes, continue? (optional save-all before exit)", DefaultConfirmation.NO)
                    ?: return Unit
            if (requestConfirmation("save/push all unsaved changes?", DefaultConfirmation.YES)) {
                println("save/pushing on close...")
                for ((name, folder) in pulledFolders.entries) {
                    doPush(name, folder)
                }
                println("save/pushed ${pulledFolders.size} group(s)")
            }
        }
        return null
    }

    private fun create(name: String?) {
        val folderName = name ?: askForFolderName() ?: return
        if (db.hasFolder(folderName)) {
            return if (requestConfirmation(
                    "group '$folderName' already exists, view/pull instead?",
                    DefaultConfirmation.YES
                )
            )
                pull(folderName)
            else
                println("nothing changed")
        }
        confirm("create group '$folderName'?", DefaultConfirmation.YES) ?: return

        pulledFolders[folderName] = DBFolder(folderPath(folderName))
    }
    private fun delete(name: String?) {
        val folderName = name ?: askForFolderName() ?: return
        strongConfirm("PERMANENTLY DELETE group '$folderName'?") ?: return

        pulledFolders[folderName]?.delete()
        db[folderName].delete()
    }

    private fun doPush(folderName: String, pulledFolder: DBFolder) {
        println("- push '$folderName'")
        db[folderName][AllFolderContents] = pulledFolder[AllFolderContents]
        pulledFolder.delete()
    }
    private fun doPush(folderName: String) {
        doPush(folderName,pulledFolders[folderName]!!)
        pulledFolders.remove(folderName)
    }
    private fun doPull(folderName: String) {
        println("- pull '$folderName'")
        val pulledFolder = pulledFolders[folderName] ?: DBFolder(folderPath(folderName))
        pulledFolders[folderName] = pulledFolder
        pulledFolder[AllFolderContents] = db[folderName][AllFolderContents]
    }

    private fun push(name: String?) {
        val folderName = name ?: askForFolderName() ?: return
        if (!detectPulledFolder(folderName)) {
            println("group '$folderName' is not being edited")
            return
        }
        confirm("save/push group '$folderName'?", DefaultConfirmation.YES) ?: return

        doPush(folderName)
    }
    private fun pull(name: String?) {
        val folderName = name ?: askForFolderName() ?: return

        if (!db.hasFolder(folderName))
            confirm("no such group '$folderName' in database, create it?", DefaultConfirmation.NO)
                ?: return println("cancelled")

        if (detectPulledFolder(folderName)) {
            confirm("revert group '$folderName' to last saved value?", DefaultConfirmation.NO)
                ?: return println("nothing changed")
        } else {
            confirm("view/pull group '$folderName'?", DefaultConfirmation.YES) ?: return
        }

        doPull(folderName)
    }
    private fun listOpened() {
        println("currently viewing:")
        if (pulledFolders.isEmpty())
            println("<none>")
        else
            println(
                pulledFolders.keys.joinToString(", ") { "'$it'" }
            )
    }

    private fun deleteDatabase():Unit? {
        return if (requestStrongConfirmation("delete the ENTIRE DATABASE?")) {
            db.destroy()
            println("DELETED DATABASE")
            null // <- tell the loop to break by returning null
        } else
            println("canceled delete")
    }
}