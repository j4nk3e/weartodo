package io.github.juumixx.todo

import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

class Todo(txt: String? = null) {
    val tasks = ArrayList<Task>()

    init {
        txt?.let {
            tasks.addAll(read(it))
        }
    }

    fun readLine(line: String) {
        if (line.isNotBlank()) {
            tasks.add(Task(line))
        }
    }

    fun read(txt: String) = txt.split("\n").filter(String::isNotBlank).map { Task(it) }.toMutableList()

    fun contexts() = tasks.flatMap { task -> task.contexts }.toSortedSet()
    fun projects() = tasks.flatMap { task -> task.projects }.toSortedSet()
    fun context(name: String) = tasks.filter { task -> task.contexts.contains(name) }
    fun contextString(name: String) = context(name).map(Task::toString).joinToString("\n")

    override fun toString() = tasks.map(Task::toString).joinToString("\n")
}

data class Task(val txt: String) {
    companion object {
        private val COMPLETED = Regex("^x\\s+(.+)")
        private val PRIORITY = Regex("^\\(([A-Z])\\)\\s+(.+)")
        private val COMPLETION_DATE = Regex("^x\\s+([0-9]{4}-[0-9]{2}-[0-9]{2})\\s+([0-9]{4}-[0-9]{2}-[0-9]{2})\\s+(.+)")
        private val CREATION_DATE = Regex("^(\\([A-Z]\\)\\s+)?([0-9]{4}-[0-9]{2}-[0-9]{2})\\s+(.+)")
        private val CREATION_DATE_COMPLETED = Regex("^x\\s+([0-9]{4}-[0-9]{2}-[0-9]{2})\\s+(.+)")
        private val PROJECT = Regex("\\+(\\S+)")
        private val CONTEXT = Regex("@(\\S+)")
        private val TAG = Regex("([^\\s:]+):([^\\s:]+)")
        val DATE_STRING = "yyyy-MM-dd"
        val DATE_FORMAT = DateTimeFormat.forPattern(DATE_STRING)!!
    }

    private val trim = txt.trim()
    val completed = COMPLETED.matches(trim)
    val priority = PRIORITY.matchEntire(trim)?.groupValues?.get(1)
    val completionDate: LocalDateTime?
    val creationDate: LocalDateTime?
    val content: String
    val projects = ArrayList<String>()
    val contexts = ArrayList<String>()
    val tags = ArrayList<Pair<String, String>>()

    init {
        if (completed) {
            val completionMatch = COMPLETION_DATE.matchEntire(trim)
            if (completionMatch != null) {
                completionDate = completionMatch.groupValues[1].let { LocalDateTime.parse(it, DATE_FORMAT) }
                creationDate = completionMatch.groupValues[2].let { LocalDateTime.parse(it, DATE_FORMAT) }
                content = completionMatch.groupValues[3]
            } else {
                val creationMatch = CREATION_DATE_COMPLETED.matchEntire(trim)
                if (creationMatch != null) {
                    completionDate = null
                    creationDate = creationMatch.groupValues[1].let { LocalDateTime.parse(it, DATE_FORMAT) }
                    content = creationMatch.groupValues[2]
                } else {
                    completionDate = null
                    creationDate = null
                    content = COMPLETED.matchEntire(trim)!!.groupValues[1]
                }
            }
        } else {
            completionDate = null
            val creationMatch = CREATION_DATE.matchEntire(trim)
            if (creationMatch != null) {
                creationDate = creationMatch.groupValues[2].let { LocalDateTime.parse(it, DATE_FORMAT) }
                content = creationMatch.groupValues[3]
            } else {
                creationDate = null
                val priorityMatch = PRIORITY.matchEntire(trim)
                if (priorityMatch != null) {
                    content = priorityMatch.groupValues[2]
                } else {
                    content = trim
                }
            }
        }
        content.split(" ").forEach { checkSpecial(it) }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (completed) {
            sb.append("x ")
        } else if (priority != null) {
            sb.append("($priority) ")
        }
        if (completionDate != null) {
            sb.append("${completionDate.toString(DATE_FORMAT)} ")
        }
        if (creationDate != null) {
            sb.append("${creationDate.toString(DATE_FORMAT)} ")
        }
        sb.append(content)
        return sb.toString()
    }

    private fun checkSpecial(word: String): Boolean {
        if (word.isBlank()) {
            return false
        }
        val projectMatch = PROJECT.matchEntire(word)
        if (projectMatch != null) {
            projects.add(projectMatch.groupValues[1])
            return false
        }
        val contextMatch = CONTEXT.matchEntire(word)
        if (contextMatch != null) {
            contexts.add(contextMatch.groupValues[1])
            return false
        }
        val tagMatch = TAG.matchEntire(word)
        if (tagMatch != null) {
            val key = tagMatch.groupValues[1]
            val value = tagMatch.groupValues[2]
            tags.add(Pair(key, value))
            return false
        }
        return true
    }
}