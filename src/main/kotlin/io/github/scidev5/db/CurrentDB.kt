package io.github.scidev5.db

import io.github.scidev5.commandLine.*
import io.github.scidev5.commandLine.DefaultConfirmation
import io.github.scidev5.db.encryption.DBEncryption
import io.github.scidev5.util.fillet
import io.github.scidev5.workingDir
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import kotlin.io.path.*

private val PARAM_DELIMITER = Regex.fromLiteral(",")
private const val WILDCARD = "*"

class CurrentDB(password:String=AskForInput.password("enter database password")) {
    private enum class DBCommand(val label:String) {
        TEXTS       ("help"),
        CLOSE       ("close"),
        LIST_PULLED ("list viewing"),
        SAVE        ("save"),
        VIEW        ("view"),
        CREATE      ("create"),
        DELETE      ("delete"),
        DELETE_DB   ("delete database"),
        PAUSE       ("pause"),
        PRINT       ("print");

        companion object {
            fun parse(string: String): DBCommand? {
                return all.firstOrNull { it.label == string }
                    ?: when (string) {
                        "h", "?"    -> TEXTS
                        "x"         -> CLOSE
                        "s", "push" -> SAVE
                        "v", "pull" -> VIEW
                        "c"         -> CREATE
                        "del"       -> DELETE
                        "l", "list" -> LIST_PULLED
                        "..."       -> PAUSE
                        "!"         -> PRINT
                        else -> null
                    }
            }
            val all = arrayOf(
                TEXTS, CLOSE, LIST_PULLED, SAVE, VIEW, CREATE, DELETE, DELETE_DB, PAUSE, PRINT
            )
        }
    }
    private class DBCommandParamPair(val command: DBCommand?, val params: List<String>)
    private enum class DBCommandSource {
        USER, CLI
    }

    companion object {
        fun init() {
            val config = DBConfig()

            val remoteDirIn = AskForInput.lineOrNevermind(
                "enter storage folder path (right click to paste)\n  ",
                "valid path (relative or absolute), type \"default\","
            ) {
                try {
                    Paths.get(it); true
                } catch (e: InvalidPathException) {
                    false
                }
            } ?: return println("nothing changed")
            if (remoteDirIn != "default" && remoteDirIn.isNotEmpty())
                config.remoteDir = Paths.get(remoteDirIn

                )
            if (!config.remoteDir.isDirectory())
                config.remoteDir.createDirectories()

            config.save()

            while (true) {
                val password = DBEncryption.enterNewPassword()
                val passwordData = DBEncryption(password, config.remoteDir)
                passwordData.initPassword()
                break
            }

            DBInitData.writeAllDotfiles(workingDir)

            println("""
initialized coded-file-db:
 [ local  ] ${workingDir.absolutePathString()}
 [ remote ] ${config.remoteDir.absolutePathString()}
""".trim())
        }

        fun resetPassword() {
            val db = CurrentDB()
            db.requireUsableOrElse() ?: return
            db.remote.encryption.setPassword(DBEncryption.enterNewPassword())
            db.close(allowAbort = false)
            println("password successfully changed")
        }

        fun run(commandListString: String) {
            if (!ProgramArguments.autoConfirmFlag)
                println("'-y' was not set, manual confirmation will be required at each command.")

            val commands = commandListString.split(Regex.fromLiteral(";")).map { it.trim() }

            run withDBOpen@{
                val db = CurrentDB()
                db.requireUsableOrElse() ?: return@withDBOpen
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
            db.requireUsableOrElse() ?: return

            loopWithDBOpen@ while (true) {
                val input = AskForInput.obj(
                    "[:: enter command ::]",
                    Companion::parseCommand
                )
                db.runCommand(input, DBCommandSource.USER)
                    ?: break@loopWithDBOpen
            }
        }

        private fun parseCommand(lineIn:String):DBCommandParamPair {
            val (rawCommand, rawParam) = lineIn.split(Regex.fromLiteral(":"), 2) + listOf("")
            return DBCommandParamPair(
                DBCommand.parse(rawCommand.trim()),
                rawParam.split(PARAM_DELIMITER).map { it.trim() }
            )
        }
    }
    private fun runCommand(input:DBCommandParamPair?, source: DBCommandSource):Unit? {
        return when (input?.command) {
            DBCommand.TEXTS -> Help.printDBOpen()

            DBCommand.CREATE -> create(input.params)
            DBCommand.DELETE -> delete(input.params)
            DBCommand.SAVE -> push(input.params)
            DBCommand.VIEW -> pull(input.params)

            DBCommand.LIST_PULLED -> listOpened()

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
    fun requireUsableOrElse():Unit? {
        return if (!config.initialized) {
            println(">> no config present in working directory.\n>> please navigate to a directory linked to a database.")
            null
        } else if (!remote.encryption.checkPassword()) {
            println(">> password for database was incorrect")
            if (requestConfirmation("show incorrectly entered password?",DefaultConfirmation.NO))
                remote.encryption.logIncorrectPassword()
            null
        } else Unit
    }

    private val remote: DBRemote = DBRemote(config.remoteDir,config,password)
    private val pulledFolders = HashMap<String, DBFolder>()
    private fun askForFolderNames() = AskForInput.lineOrNevermind(
        "file group name?",
        "group name"
    ) { it.isNotBlank() }?.trim()?.split(PARAM_DELIMITER)
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
    private fun parseFolderList(
        paramsIn: List<String>,
        allowWildcard:Boolean = true,
        all:Set<String>? = null,
        askIfNone:Boolean = false
    ):Set<String>? {
        val params =
            (if (allowWildcard) paramsIn.filter { it != WILDCARD } else paramsIn)
            .map { it.replace(Regex("[^a-zA-Z\\d-_.()\\[\\]~ ]"),"_").trim() }
            .filter { it.isNotBlank() }
        return if (paramsIn.contains(WILDCARD) && allowWildcard)
            ((all ?: remote.allFolders()).filterNot { it.isBlank() } + params).toSet()
        else if (params.isEmpty()) {
            if (askIfNone)
                askForFolderNames()?.let {
                    parseFolderList(it,allowWildcard,all)
                }
            else null
        } else
            params.toSet()
    }

    private fun foldersToString(arr:Collection<String>):String
        = "[${arr.joinToString(", "){"'$it'"}}]"


    private fun close(allowAbort:Boolean = true):Unit? {
        if (pulledFolders.isNotEmpty()) {
            if (allowAbort)
                confirm("unsaved changes, continue closing?    (optional save-all next)\n", DefaultConfirmation.NO)
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

    private fun create(names: List<String>) {
        val folderNames = parseFolderList(
            names,
            allowWildcard = false,
            askIfNone = true
        ) ?: return
        val (existingFolderNames, creatingFolderNames)
            = fillet(folderNames) { remote.hasFolder(it) }

        if (existingFolderNames.isNotEmpty()) {
            if (requestConfirmation(
                    "groups ${foldersToString(existingFolderNames)} already exist, view/pull instead?",
                    DefaultConfirmation.YES
                )
            )
                pull(existingFolderNames)
            else
                println("nothing changed")
        }
        if (creatingFolderNames.isNotEmpty()) {
            confirm(
                "create groups ${foldersToString(creatingFolderNames)}?",
                DefaultConfirmation.YES
            ) ?: return

            for (folderName in creatingFolderNames)
                pulledFolders[folderName] = DBFolder(folderPath(folderName))
        }
    }
    private fun delete(names: List<String>) {
        val folderNames = parseFolderList(
            names,
            askIfNone = true
        ) ?: return
        strongConfirm("PERMANENTLY DELETE groups ${foldersToString(folderNames)}?")
            ?: return

        for (folderName in folderNames) {
            pulledFolders[folderName]?.delete()
            remote[folderName].delete()
        }
    }

    private fun doPush(folderName: String, pulledFolder: DBFolder) {
        println("- push '$folderName'")
        remote[folderName][AllFolderContents] = pulledFolder[AllFolderContents]
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
        pulledFolder[AllFolderContents] = remote[folderName][AllFolderContents]
    }

    private fun push(names: List<String>) {
        val folderNames = parseFolderList(
            names,
            all = pulledFolders.keys,
            askIfNone = true
        ) ?: return
        val (pulledFolderNames,remoteFolderNames)
            = fillet(folderNames,this::detectPulledFolder)

        if (remoteFolderNames.isNotEmpty()) {
            println("groups ${foldersToString(remoteFolderNames)} are not being edited, skipping")
        }
        if (pulledFolderNames.isNotEmpty()) {
            confirm("save/push groups ${foldersToString(pulledFolderNames)}?", DefaultConfirmation.YES) ?: return
            pulledFolderNames.forEach(this::doPush)
        }
    }
    private fun pull(names: List<String>) {
        var folderNames = parseFolderList(
            names,
            all = remote.allFolders() - pulledFolders.keys,
            askIfNone = true
        ) ?: return

        val (existingFolderNames, creatingFolderNames)
                = fillet(folderNames) { remote.hasFolder(it) }
        if (creatingFolderNames.isNotEmpty())
            confirm(
                "no such groups ${foldersToString(creatingFolderNames)} in database, create?",
                DefaultConfirmation.NO
            ) ?: run { // otherwise, only modify existing
                folderNames = existingFolderNames.toSet()
            }

        val (pulledFolderNames, notPulledFolderNames)
                = fillet(folderNames, this::detectPulledFolder)
        if (pulledFolderNames.isNotEmpty())
            confirm(
                "revert groups ${foldersToString(pulledFolderNames)} to last saved value?",
                DefaultConfirmation.NO
            ) ?: run { // otherwise, only pull not already pulled
                folderNames = notPulledFolderNames.toSet()
            }


        confirm("view/pull groups ${foldersToString(folderNames)}?", DefaultConfirmation.YES) ?: return

        folderNames.forEach(this::doPull)
    }
    private fun listOpened() {
        println("currently viewing:")
        if (pulledFolders.isEmpty())
            println("<none>")
        else
            println(foldersToString(pulledFolders.keys))
    }

    private fun deleteDatabase():Unit? {
        return if (requestStrongConfirmation("delete the ENTIRE DATABASE?")) {
            remote.destroy()
            println("DELETED DATABASE")
            null // <- tell the loop to break by returning null
        } else
            println("canceled delete")
    }
}