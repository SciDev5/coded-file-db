package io.github.scidev5.db

import io.github.scidev5.commandLine.*
import io.github.scidev5.commandLine.DefaultConfirmation
import io.github.scidev5.workingDir
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*


class OpenDB {
    private enum class DBCommand(val label:String) {
        HELP      ("help"),
        CLOSE     ("close"),
        LIST_OPEN ("list opened"),
        SAVE      ("save"),
        VIEW      ("view"),
        CREATE    ("create"),
        DELETE    ("delete"),
        DELETE_DB ("delete database");

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
                        else -> null
                    }
            }
            val all = arrayOf(HELP,CLOSE,SAVE,VIEW,CREATE,DELETE,DELETE_DB,LIST_OPEN)
        }
    }
    private class DBCommandParamPair(val command: DBCommand?, val params: List<String>)

    companion object {
        fun initDB() {
            val path = AskForInput.lineOrNevermind(
                "enter the database storage folder path (right click to paste)",
                "valid path"
            ) {
                try {
                    Paths.get(it); true
                } catch (e: InvalidPathException) {
                    false
                }
            }
                ?: return println("cancelled")

            val config = DBConfig()
            config.remoteDir = Paths.get(path)
            config.save()

            val jv = Companion::class.java
            val jarPath = Paths.get(jv.protectionDomain.codeSource.location.toURI()).absolutePathString().replace("\\","\\\\")
            val openShContent = (
                    jv.getResourceAsStream("/autos/open.sh")
                        ?: throw Exception("'/autos/open.sh' template resource missing")
                    ).bufferedReader().readText()
                .replace(Regex("\\{\\{JAR_PATH}}"),jarPath)
            val openShPath = workingDir / "open.sh"
            openShPath.writeText(openShContent)
            try {
                openShPath.setPosixFilePermissions(
                    setOf(PosixFilePermission.OWNER_EXECUTE) + openShPath.getPosixFilePermissions()
                )
            } catch (_: UnsupportedOperationException) {}

            if (!ProgramArguments.autoConfirmFlag) {
                println("done, press any key to continue...")
                System.`in`.read()
            }
        }
    }

    private val config = DBConfig()
    private val db: DBRemote = DBRemote(config.remoteDir)

    private val pulledFolders = HashMap<String, DBFolder>()

    private fun askForFolderName() = AskForInput.lineOrNevermind(
        "file group name?: ",
        "3+ characters"
    ) { it.length >= 3 }
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

    private fun close():Unit? {
        if (pulledFolders.isNotEmpty()) {
            confirm("unsaved changes, continue? (optional save-all before exit)", DefaultConfirmation.NO)
                ?: return Unit
            if (requestConfirmation("save/push all unsaved changes?", DefaultConfirmation.YES)) {
                println("save/pushing...")
                for ((name,folder) in pulledFolders.entries) {
                    println(" - '$name'")
                    doPush(name,folder)
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
        db[folderName][AllFolderContents] = pulledFolder[AllFolderContents]
        pulledFolder.delete()
    }
    private fun doPush(folderName: String) {
        doPush(folderName,pulledFolders[folderName]!!)
        pulledFolders.remove(folderName)
    }
    private fun doPull(folderName: String) {
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


    init {
        if (!config.initialized) {
            println(">> no config present in working directory.\n>> please navigate to a directory linked to a database.")
        } else loop@while (true) {
            val input = AskForInput.obj(
                "enter command"
            ) { lineIn ->
                val (rawCommand, rawParam) = lineIn.split(Regex(":"), 2) + listOf("")
                return@obj DBCommandParamPair(
                    DBCommand.parse(rawCommand.trim()),
                    rawParam.split(Regex("/")).map { it.trim() }
                )
            }
            when(input?.command) {
                DBCommand.HELP -> Help.printDBOpen()
                DBCommand.CLOSE -> close() ?: break@loop

                DBCommand.CREATE -> execForeachParam(input.params,this::create)
                DBCommand.DELETE -> execForeachParam(input.params,this::delete)
                DBCommand.SAVE -> execForeachParam(input.params,this::push)
                DBCommand.VIEW -> execForeachParam(input.params,this::pull)

                DBCommand.LIST_OPEN -> listOpened()

                DBCommand.DELETE_DB -> deleteDatabase() ?: break@loop

                else -> println("invalid command, try one of [${DBCommand.all.joinToString(", ") { "'${it.label}'" }}]")
            }
        }
    }
}