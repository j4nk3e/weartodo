package io.github.juumixx.todo

import io.github.juumixx.todo.Task.Companion.DATE_STRING
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.joda.time.JodaTimePermission
import org.junit.Before
import org.junit.Test

class TodoTest {
    @Test
    fun shouldReadTodoPerLine() {
        val txt = """task 1
x task (2) is already done
(A) 04-05-2003 task x 3 @context
(B) task 4 @context +project
X task 5 +project

task 6 is not priority (A) but due:01-02-2003
x 03-04-2003 04-05-2003 task 7
(A) task 8 @ctx
(B) task 9
(C) 04-05-2003 task 10
"""
        val todo = Todo(txt)
        todo.tasks.size shouldBe 10
        todo.tasks.joinToString("\n") shouldEqual txt.replace("\n\n", "\n").trim()
    }

    @Test
    fun shouldReadCompletedTask() {
        Task("x task (2) is already done").completed shouldBe true
        Task("(A) 04-05-2003 task x 3 @context").completed shouldBe false
        Task("X task 5 +project").completed shouldBe false
    }

    @Test
    fun shouldParsePriority() {
        Task("(A) 04-05-2003 task x 3 @context").priority shouldEqual "A"
        Task("(B) task 4 @context +project").priority shouldEqual "B"
        Task("X task 5 +project").priority shouldBe null
        Task("(A)07-06-2017 no date or priority").priority shouldBe null
    }

    @Test
    fun shouldParseCreationDate() {
        Task("x 06-06-2017 05-06-2017 task 3 @context").creationDate?.toString(DATE_STRING) shouldEqual "05-06-2017"
        Task("07-06-2017 task was created today").creationDate?.toString(DATE_STRING) shouldEqual "07-06-2017"
        Task("(A) 06-06-2017 task x 2 +project").creationDate?.toString(DATE_STRING) shouldEqual "06-06-2017"
        Task("(A)07-06-2017 no date or priority").creationDate shouldBe null
        Task("is not priority (A) but due:01-02-2003").creationDate shouldBe null
        Task("x do 01-02-2003 something").creationDate shouldBe null
    }

    @Test
    fun shouldParseCompletionDate() {
        Task("x 06-06-2017 07-06-2017 task was created today").completionDate?.toString(DATE_STRING) shouldEqual "06-06-2017"
        Task("x 01-06-2017 07-12-2016 task was done +01-02-2003").completionDate?.toString(DATE_STRING) shouldEqual "01-06-2017"
        Task("x 05-06-2017 task 3 @context").completionDate?.toString(DATE_STRING) shouldBe null
        Task("(A) 06-06-2017 07-06-2017 task x 2 +project").completionDate shouldBe null
        Task("(A)07-06-2017 no date or priority").completionDate shouldBe null
        Task("is not priority (A) but due:01-02-2003").completionDate shouldBe null
        Task("(B) do 01-02-2003 something").completionDate shouldBe null
    }

    @Test
    fun shouldParseProjects() {
        Task("(A) 04-05-2003 task x 3 @context").projects.size shouldBe 0
        Task("(B) task 4 @context +project").projects shouldEqual arrayListOf("project")
        Task("X task 5 +project").projects shouldEqual arrayListOf("project")
        Task("(A)07-06-2017 no +date or +priority").projects shouldEqual arrayListOf("date", "priority")
    }

    @Test
    fun shouldParseContexts() {
        Task("X task 5 +project").contexts.size shouldBe 0
        Task("(A) 04-05-2003 task x 3 @context").contexts shouldEqual arrayListOf("context")
        Task("(B) task 4 @context +project").contexts shouldEqual arrayListOf("context")
        Task("(A)07-06-2017 no @date or @priority").contexts shouldEqual arrayListOf("date", "priority")
    }

    @Test
    fun shouldParseTags() {
        Task("is not priority (A) but due: 01-02-2003").tags.size shouldBe 0
        Task("(A) task 8 @ctx").tags.size shouldBe 0
        Task("01-02-2017 is not priority (A) but due:01-02-2003").tags shouldEqual arrayListOf(Pair("due", "01-02-2003"))
        Task("any:tag can:be any:where").tags shouldEqual arrayListOf(Pair("any", "tag"), Pair("can", "be"), Pair("any", "where"))
    }

    @Test
    fun shouldNotAlterContent() {
        Task("X task 5 +project").content shouldEqual "X task 5 +project"
        Task("(A) 04-05-2003 task x 3 @context").content shouldEqual "task x 3 @context"
        Task("x task 4 @context +project").content shouldEqual "task 4 @context +project"
        Task("(A)07-06-2017 no @date or @priority").content shouldEqual "(A)07-06-2017 no @date or @priority"
    }
}