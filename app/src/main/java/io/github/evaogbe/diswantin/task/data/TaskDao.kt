package io.github.evaogbe.diswantin.task.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Dao
interface TaskDao {
    @Query(
        """SELECT
            t.*,
            td.scheduled_date AS scheduled_date_priority,
            td.scheduled_time AS scheduled_time_priority,
            td.deadline_date AS deadline_date_priority,
            td.deadline_time AS deadline_time_priority,
            rd.task_id IS NOT NULL AS recurring_priority,
            td.start_after_date AS start_after_date_priority,
            td.start_after_time AS start_after_time_priority,
            td.created_at AS created_at_priority,
            td.id AS id_priority
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        JOIN (
            SELECT p.descendant, MAX(p.depth) AS depth
            FROM task_path p
            WHERE p.ancestor IN (
                SELECT t.id
                FROM task t
                LEFT JOIN (
                    SELECT task_id, MAX(done_at) AS done_at
                    FROM task_completion
                    GROUP BY task_id
                ) c ON c.task_id = t.id
                LEFT JOIN task_recurrence r ON r.task_id = t.id
                LEFT JOIN (
                    SELECT task_id, MAX(skipped_at) AS skipped_at
                    FROM task_skip
                    GROUP BY task_id
                ) s ON s.task_id = t.id
                WHERE (c.done_at IS NULL OR (r.task_id IS NOT NULL AND c.done_at < :startOfToday))
                    AND (
                        t.scheduled_date IS NULL
                        OR t.scheduled_date < :today
                        OR (
                            t.scheduled_date = :today
                            AND (t.scheduled_time IS NULL OR t.scheduled_time <= :currentTime)
                        )
                    )
                    AND (
                        r.task_id IS NULL
                            OR t.scheduled_time IS NULL
                            OR t.scheduled_time <= :currentTime
                    )
                    AND (t.start_after_date IS NULL OR t.start_after_date <= :today)
                    AND (t.start_after_time IS NULL OR t.start_after_time <= :currentTime)
                    AND (s.skipped_at IS NULL OR s.skipped_at < :startOfToday)
                    AND (r.start IS NULL OR r.start <= :today)
                    AND CASE r.type
                        WHEN 0 THEN (julianday(:today) - julianday(r.start)) % r.step = 0
                        WHEN 1 THEN (julianday(:today) - julianday(r.start)) % (r.step * 7) = 0
                        WHEN 2 THEN (
                                12
                                + CAST(strftime('%m', :today) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%d', r.start) = strftime('%d', :today)
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '03-31', '05-31', '07-31', '08-31', '10-31',
                                            '12-31'
                                        )
                                    AND strftime('%m-%d', :today)
                                        IN ('04-30', '06-30', '09-30', '11-30')
                                )
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '02-29', '03-31', '04-30', '05-31', '06-30',
                                            '07-31', '08-31', '09-30', '10-31', '11-30', '12-31'
                                        )
                                    AND (
                                        strftime('%m-%d', :today) = '02-29'
                                        OR (
                                            strftime('%m-%d', :today) = '02-28'
                                            AND (
                                                CAST(strftime('%Y', :today) as INT) & 3 != 0
                                                OR (
                                                    CAST(strftime('%Y', :today) as INT) % 25 = 0
                                                    AND CAST(strftime('%Y', :today) as INT) & 15
                                                        != 0
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        WHEN 3 THEN (
                                12
                                + CAST(strftime('%m', :today) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND CAST((CAST(strftime('%d', :today) as REAL) / 7) as INT)
                                + (
                                    (CAST(strftime('%d', :today) as REAL) / 7)
                                    > CAST((CAST(strftime('%d', :today) as REAL) / 7) as INT)
                                )
                                = CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    + (
                                        (CAST(strftime('%d', r.start) as REAL) / 7)
                                        > CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    )
                            AND strftime('%w', r.start) = strftime('%w', :today)
                        WHEN 4 THEN (
                                CAST(strftime('%Y', :today) as INT)
                                - CAST(strftime('%Y', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%m-%d', r.start) = strftime('%m-%d', :today)
                                OR (
                                    strftime('%m-%d', r.start) = '02-29'
                                    AND strftime('%m-%d', :today) = '02-28'
                                    AND (
                                        CAST(strftime('%Y', :today) as INT) & 3 != 0
                                        OR (
                                            CAST(strftime('%Y', :today) as INT) % 25 = 0
                                            AND CAST(strftime('%Y', :today) as INT) & 15 != 0
                                        )
                                    )
                                )
                            )
                        ELSE TRUE
                        END
            )
                AND p.descendant IN (
                    SELECT t.id
                    FROM task t
                    LEFT JOIN (
                        SELECT task_id, MAX(done_at) AS done_at
                        FROM task_completion
                        GROUP BY task_id
                    ) c ON c.task_id = t.id
                    LEFT JOIN task_recurrence r ON r.task_id = t.id
                    LEFT JOIN (
                        SELECT task_id, MAX(skipped_at) AS skipped_at
                        FROM task_skip
                        GROUP BY task_id
                    ) s ON s.task_id = t.id
                    WHERE (
                        c.done_at IS NULL
                        OR (r.task_id IS NOT NULL AND c.done_at < :startOfToday)
                    )
                        AND (
                            t.scheduled_date IS NULL
                            OR t.scheduled_date < :today
                            OR (
                                t.scheduled_date = :today
                                AND (t.scheduled_time IS NULL OR t.scheduled_time <= :currentTime)
                            )
                        )
                        AND (
                            r.task_id IS NULL
                                OR t.scheduled_time IS NULL
                                OR t.scheduled_time <= :currentTime
                        )
                        AND (t.start_after_date IS NULL OR t.start_after_date <= :today)
                        AND (t.start_after_time IS NULL OR t.start_after_time <= :currentTime)
                        AND (s.skipped_at IS NULL OR s.skipped_at < :startOfToday)
                        AND (r.start IS NULL OR r.start <= :today)
                        AND CASE r.type
                            WHEN 0 THEN (julianday(:today) - julianday(r.start)) % r.step = 0
                            WHEN 1 THEN (julianday(:today) - julianday(r.start)) % (r.step * 7) = 0
                            WHEN 2 THEN (
                                    12
                                    + CAST(strftime('%m', :today) as INT)
                                    - CAST(strftime('%m', r.start) as INT)
                                ) % r.step = 0
                                AND (
                                    strftime('%d', r.start) = strftime('%d', :today)
                                    OR (
                                        strftime('%m-%d', r.start)
                                            IN (
                                                '01-31', '03-31', '05-31', '07-31', '08-31',
                                                '10-31', '12-31'
                                            )
                                        AND strftime('%m-%d', :today)
                                            IN ('04-30', '06-30', '09-30', '11-30')
                                    )
                                    OR (
                                        strftime('%m-%d', r.start)
                                            IN (
                                                '01-31', '02-29', '03-31', '04-30', '05-31',
                                                '06-30', '07-31', '08-31', '09-30', '10-31',
                                                '11-30', '12-31'
                                            )
                                        AND (
                                            strftime('%m-%d', :today) = '02-29'
                                            OR (
                                                strftime('%m-%d', :today) = '02-28'
                                                AND (
                                                    CAST(strftime('%Y', :today) as INT) & 3 != 0
                                                    OR (
                                                        CAST(strftime('%Y', :today) as INT) % 25 = 0
                                                        AND CAST(strftime('%Y', :today) as INT) & 15
                                                            != 0
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            WHEN 3 THEN (
                                    12
                                    + CAST(strftime('%m', :today) as INT)
                                    - CAST(strftime('%m', r.start) as INT)
                                ) % r.step = 0
                                AND CAST((CAST(strftime('%d', :today) as REAL) / 7) as INT)
                                    + (
                                        (CAST(strftime('%d', :today) as REAL) / 7)
                                        > CAST((CAST(strftime('%d', :today) as REAL) / 7) as INT)
                                    )
                                    = CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                        + (
                                            (CAST(strftime('%d', r.start) as REAL) / 7)
                                            > CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                        )
                                AND strftime('%w', r.start) = strftime('%w', :today)
                            WHEN 4 THEN (
                                    CAST(strftime('%Y', :today) as INT)
                                    - CAST(strftime('%Y', r.start) as INT)
                                ) % r.step = 0
                                AND (
                                    strftime('%m-%d', r.start) = strftime('%m-%d', :today)
                                    OR (
                                        strftime('%m-%d', r.start) = '02-29'
                                        AND strftime('%m-%d', :today) = '02-28'
                                        AND (
                                            CAST(strftime('%Y', :today) as INT) & 3 != 0
                                            OR (
                                                CAST(strftime('%Y', :today) as INT) % 25 = 0
                                                AND CAST(strftime('%Y', :today) as INT) & 15 != 0
                                            )
                                        )
                                    )
                                )
                            ELSE TRUE
                            END
                )
            GROUP BY p.descendant
        ) leaf ON leaf.descendant = p.descendant AND leaf.depth = p.depth
        JOIN task td ON p.descendant = td.id
        LEFT JOIN (SELECT DISTINCT task_id FROM task_recurrence) rd ON rd.task_id = td.id
        ORDER BY
            t.scheduled_date IS NULL,
            t.scheduled_date,
            t.scheduled_time IS NULL,
            t.scheduled_time,
            scheduled_date_priority IS NULL,
            scheduled_date_priority,
            scheduled_time_priority IS NULL,
            scheduled_time_priority,
            recurring_priority DESC,
            t.deadline_date IS NULL,
            t.deadline_time IS NULL,
            deadline_date_priority IS NULL,
            deadline_time_priority IS NULL,
            start_after_time_priority IS NOT NULL,
            deadline_date_priority,
            deadline_time_priority,
            start_after_date_priority,
            start_after_time_priority,
            created_at_priority,
            id_priority
        LIMIT 20"""
    )
    fun getTaskPriorities(
        today: LocalDate,
        currentTime: LocalTime,
        startOfToday: Instant,
    ): Flow<List<TaskPriority>>

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<Task>

    @Query(
        """SELECT
            t.id,
            t.name,
            t.note,
            t.deadline_date,
            t.deadline_time,
            t.start_after_date,
            t.start_after_time,
            t.scheduled_date,
            t.scheduled_time,
            com.done_at,
            t.category_id,
            cat.name AS category_name,
            tp.id AS parent_id,
            tp.name AS parent_name,
            rp.task_id IS NOT NULL AS parent_recurring,
            comp.done_at AS parent_done_at
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) com ON com.task_id = t.id
        LEFT JOIN task_category cat ON cat.id = t.category_id
        LEFT JOIN task_path p ON p.descendant = t.id AND p.depth = 1
        LEFT JOIN task tp ON tp.id = p.ancestor
        LEFT JOIN (SELECT DISTINCT task_id FROM task_recurrence) rp ON rp.task_id = tp.id
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) comp ON comp.task_id = tp.id
        WHERE t.id = :id
        LIMIT 1"""
    )
    fun getTaskDetailById(id: Long): Flow<TaskDetail?>

    @Query("SELECT * FROM task WHERE category_id = :categoryId ORDER BY name")
    fun getTasksByCategoryId(categoryId: Long): PagingSource<Int, Task>

    @Query(
        """SELECT t.id, t.name, r.task_id IS NOT NULL AS recurring, c.done_at
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        LEFT JOIN (SELECT DISTINCT task_id FROM task_recurrence) r ON r.task_id = t.id
        WHERE t.category_id = :categoryId
        ORDER BY
            c.done_at IS NOT NULL AND (r.task_id IS NULL OR c.done_at >= :startOfToday),
            c.done_at,
            t.scheduled_date IS NULL,
            t.scheduled_date,
            t.scheduled_time IS NULL,
            t.scheduled_time,
            r.task_id IS NULL,
            t.deadline_date IS NULL,
            t.deadline_date,
            t.deadline_time IS NULL,
            t.deadline_time,
            t.start_after_date,
            t.start_after_time,
            t.created_at,
            t.id"""
    )
    fun getTaskItemsByCategoryId(
        categoryId: Long,
        startOfToday: Instant
    ): PagingSource<Int, TaskItemData>

    @Query(
        """SELECT DISTINCT task.*
        FROM task
        JOIN task_fts tf ON tf.name = task.name
        WHERE task_fts MATCH :query
        LIMIT 20"""
    )
    fun search(query: String): Flow<List<Task>>

    @Transaction
    @Query(
        """SELECT DISTINCT t.id, t.name, r.task_id IS NOT NULL AS recurring, c.done_at
        FROM task t
        JOIN task_fts ON task_fts.name = t.name
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        LEFT JOIN task_recurrence r ON r.task_id = t.id
        WHERE task_fts MATCH :query
            AND (
                :deadlineStartDate IS NULL
                OR :deadlineEndDate IS NULL
                OR t.deadline_date BETWEEN :deadlineStartDate AND :deadlineEndDate
                OR (t.deadline_time IS NOT NULL AND r.start <= :deadlineEndDate)
            )
            AND (
                :startAfterStartDate IS NULL
                OR :startAfterEndDate IS NULL
                OR t.start_after_date BETWEEN :startAfterStartDate AND :startAfterEndDate
                OR (t.start_after_time IS NOT NULL AND r.start <= :startAfterEndDate)
            )
            AND (
                :scheduledStartDate IS NULL
                OR :scheduledEndDate IS NULL
                OR t.scheduled_date BETWEEN :scheduledStartDate AND :scheduledEndDate
                OR (t.scheduled_time IS NOT NULL AND r.start <= :scheduledEndDate)
            )
            AND (
                :doneStart IS NULL
                OR :doneEnd IS NULL
                OR EXISTS (
                    SELECT *
                    FROM task_completion c2
                    WHERE c2.task_id = t.id AND c2.done_at BETWEEN :doneStart AND :doneEnd
                )
            )
            AND (
                :recurrenceDate IS NULL
                OR (
                    r.start <= :recurrenceDate
                    AND CASE r.type
                        WHEN 0 THEN (julianday(:recurrenceDate) - julianday(r.start)) % r.step = 0
                        WHEN 1 THEN (julianday(:recurrenceDate) - julianday(r.start)) % (r.step * 7) = 0
                        WHEN 2 THEN (
                                12
                                + CAST(strftime('%m', :recurrenceDate) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%d', r.start) = strftime('%d', :recurrenceDate)
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '03-31', '05-31', '07-31', '08-31',
                                            '10-31', '12-31'
                                        )
                                    AND strftime('%m-%d', :recurrenceDate)
                                        IN ('04-30', '06-30', '09-30', '11-30')
                                )
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '02-29', '03-31', '04-30', '05-31',
                                            '06-30', '07-31', '08-31', '09-30', '10-31',
                                            '11-30', '12-31'
                                        )
                                    AND (
                                        strftime('%m-%d', :recurrenceDate) = '02-29'
                                        OR (
                                            strftime('%m-%d', :recurrenceDate) = '02-28'
                                            AND (
                                                CAST(strftime('%Y', :recurrenceDate) as INT) & 3 != 0
                                                OR (
                                                    CAST(strftime('%Y', :recurrenceDate) as INT) % 25 = 0
                                                    AND CAST(strftime('%Y', :recurrenceDate) as INT) & 15
                                                        != 0
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        WHEN 3 THEN (
                                12
                                + CAST(strftime('%m', :recurrenceDate) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND CAST((CAST(strftime('%d', :recurrenceDate) as REAL) / 7) as INT)
                                + (
                                    (CAST(strftime('%d', :recurrenceDate) as REAL) / 7)
                                    > CAST((CAST(strftime('%d', :recurrenceDate) as REAL) / 7) as INT)
                                )
                                = CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    + (
                                        (CAST(strftime('%d', r.start) as REAL) / 7)
                                        > CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    )
                            AND strftime('%w', r.start) = strftime('%w', :recurrenceDate)
                        WHEN 4 THEN (
                                CAST(strftime('%Y', :recurrenceDate) as INT)
                                - CAST(strftime('%Y', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%m-%d', r.start) = strftime('%m-%d', :recurrenceDate)
                                OR (
                                    strftime('%m-%d', r.start) = '02-29'
                                    AND strftime('%m-%d', :recurrenceDate) = '02-28'
                                    AND (
                                        CAST(strftime('%Y', :recurrenceDate) as INT) & 3 != 0
                                        OR (
                                            CAST(strftime('%Y', :recurrenceDate) as INT) % 25 = 0
                                            AND CAST(strftime('%Y', :recurrenceDate) as INT) & 15 != 0
                                        )
                                    )
                                )
                            )
                        ELSE FALSE
                        END
                    )
            )
        ORDER BY
            recurring,
            t.scheduled_date IS NULL,
            t.scheduled_date,
            t.scheduled_time IS NULL,
            t.scheduled_time,
            t.deadline_date IS NULL,
            t.deadline_date,
            t.deadline_time IS NULL,
            t.deadline_time,
            t.start_after_date IS NULL,
            t.start_after_date,
            t.start_after_time IS NULL,
            t.start_after_time,
            c.done_at IS NOT NULL,
            c.done_at DESC,
            t.name,
            t.id"""
    )
    fun searchTaskItems(
        query: String,
        deadlineStartDate: LocalDate?,
        deadlineEndDate: LocalDate?,
        startAfterStartDate: LocalDate?,
        startAfterEndDate: LocalDate?,
        scheduledStartDate: LocalDate?,
        scheduledEndDate: LocalDate?,
        doneStart: Instant?,
        doneEnd: Instant?,
        recurrenceDate: LocalDate?,
    ): PagingSource<Int, TaskItemWithRecurrences>

    @Transaction
    @Query(
        """SELECT DISTINCT t.id, t.name, r.task_id IS NOT NULL AS recurring, c.done_at
        FROM task t
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        LEFT JOIN task_recurrence r ON r.task_id = t.id
        WHERE (
                :deadlineStartDate IS NULL
                OR :deadlineEndDate IS NULL
                OR t.deadline_date BETWEEN :deadlineStartDate AND :deadlineEndDate
                OR (t.deadline_time IS NOT NULL AND r.start <= :deadlineEndDate)
            )
            AND (
                :startAfterStartDate IS NULL
                OR :startAfterEndDate IS NULL
                OR t.start_after_date BETWEEN :startAfterStartDate AND :startAfterEndDate
                OR (t.start_after_time IS NOT NULL AND r.start <= :startAfterEndDate)
            )
            AND (
                :scheduledStartDate IS NULL
                OR :scheduledEndDate IS NULL
                OR t.scheduled_date BETWEEN :scheduledStartDate AND :scheduledEndDate
                OR (t.scheduled_time IS NOT NULL AND r.start <= :scheduledEndDate)
            )
            AND (
                :doneStart IS NULL
                OR :doneEnd IS NULL
                OR EXISTS (
                    SELECT *
                    FROM task_completion c2
                    WHERE c2.task_id = t.id AND c2.done_at BETWEEN :doneStart AND :doneEnd
                )
            )
            AND (
                :recurrenceDate IS NULL
                OR (
                    r.start <= :recurrenceDate
                    AND CASE r.type
                        WHEN 0 THEN (julianday(:recurrenceDate) - julianday(r.start)) % r.step = 0
                        WHEN 1 THEN (julianday(:recurrenceDate) - julianday(r.start)) % (r.step * 7) = 0
                        WHEN 2 THEN (
                                12
                                + CAST(strftime('%m', :recurrenceDate) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%d', r.start) = strftime('%d', :recurrenceDate)
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '03-31', '05-31', '07-31', '08-31',
                                            '10-31', '12-31'
                                        )
                                    AND strftime('%m-%d', :recurrenceDate)
                                        IN ('04-30', '06-30', '09-30', '11-30')
                                )
                                OR (
                                    strftime('%m-%d', r.start)
                                        IN (
                                            '01-31', '02-29', '03-31', '04-30', '05-31',
                                            '06-30', '07-31', '08-31', '09-30', '10-31',
                                            '11-30', '12-31'
                                        )
                                    AND (
                                        strftime('%m-%d', :recurrenceDate) = '02-29'
                                        OR (
                                            strftime('%m-%d', :recurrenceDate) = '02-28'
                                            AND (
                                                CAST(strftime('%Y', :recurrenceDate) as INT) & 3 != 0
                                                OR (
                                                    CAST(strftime('%Y', :recurrenceDate) as INT) % 25 = 0
                                                    AND CAST(strftime('%Y', :recurrenceDate) as INT) & 15
                                                        != 0
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        WHEN 3 THEN (
                                12
                                + CAST(strftime('%m', :recurrenceDate) as INT)
                                - CAST(strftime('%m', r.start) as INT)
                            ) % r.step = 0
                            AND CAST((CAST(strftime('%d', :recurrenceDate) as REAL) / 7) as INT)
                                + (
                                    (CAST(strftime('%d', :recurrenceDate) as REAL) / 7)
                                    > CAST((CAST(strftime('%d', :recurrenceDate) as REAL) / 7) as INT)
                                )
                                = CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    + (
                                        (CAST(strftime('%d', r.start) as REAL) / 7)
                                        > CAST((CAST(strftime('%d', r.start) as REAL) / 7) as INT)
                                    )
                            AND strftime('%w', r.start) = strftime('%w', :recurrenceDate)
                        WHEN 4 THEN (
                                CAST(strftime('%Y', :recurrenceDate) as INT)
                                - CAST(strftime('%Y', r.start) as INT)
                            ) % r.step = 0
                            AND (
                                strftime('%m-%d', r.start) = strftime('%m-%d', :recurrenceDate)
                                OR (
                                    strftime('%m-%d', r.start) = '02-29'
                                    AND strftime('%m-%d', :recurrenceDate) = '02-28'
                                    AND (
                                        CAST(strftime('%Y', :recurrenceDate) as INT) & 3 != 0
                                        OR (
                                            CAST(strftime('%Y', :recurrenceDate) as INT) % 25 = 0
                                            AND CAST(strftime('%Y', :recurrenceDate) as INT) & 15 != 0
                                        )
                                    )
                                )
                            )
                        ELSE FALSE
                        END
                    )
            )
        ORDER BY
            recurring,
            t.scheduled_date IS NULL,
            t.scheduled_date,
            t.scheduled_time IS NULL,
            t.scheduled_time,
            t.deadline_date IS NULL,
            t.deadline_date,
            t.deadline_time IS NULL,
            t.deadline_time,
            t.start_after_date IS NULL,
            t.start_after_date,
            t.start_after_time IS NULL,
            t.start_after_time,
            c.done_at IS NOT NULL,
            c.done_at DESC,
            t.name,
            t.id"""
    )
    fun filterTaskItems(
        deadlineStartDate: LocalDate?,
        deadlineEndDate: LocalDate?,
        startAfterStartDate: LocalDate?,
        startAfterEndDate: LocalDate?,
        scheduledStartDate: LocalDate?,
        scheduledEndDate: LocalDate?,
        doneStart: Instant?,
        doneEnd: Instant?,
        recurrenceDate: LocalDate?,
    ): PagingSource<Int, TaskItemWithRecurrences>

    @Query(
        """SELECT t.*
        FROM task t
        JOIN task_path p ON p.ancestor = t.id
        WHERE p.descendant = :id AND p.depth = 1
        LIMIT 1"""
    )
    fun getParent(id: Long): Flow<Task?>

    @Query(
        """SELECT t.id, t.name, r.task_id IS NOT NULL AS recurring, c.done_at
        FROM task t
        JOIN task_path p ON p.descendant = t.id
        LEFT JOIN (SELECT DISTINCT task_id FROM task_recurrence) r ON r.task_id = t.id
        LEFT JOIN (
            SELECT task_id, MAX(done_at) AS done_at
            FROM task_completion
            GROUP BY task_id
        ) c ON c.task_id = t.id
        WHERE p.ancestor = :id AND p.depth = 1
        ORDER BY
            c.done_at,
            t.scheduled_date IS NULL,
            t.scheduled_date,
            t.scheduled_time IS NULL,
            t.scheduled_time,
            r.task_id IS NULL,
            t.deadline_date IS NULL,
            t.deadline_date,
            t.deadline_time IS NULL,
            t.deadline_time,
            t.start_after_date,
            t.start_after_time,
            t.created_at,
            t.id"""
    )
    fun getChildren(id: Long): PagingSource<Int, TaskItemData>

    @Query("SELECT * FROM task_recurrence WHERE task_id = :taskId ORDER BY start LIMIT 7")
    fun getTaskRecurrencesByTaskId(taskId: Long): Flow<List<TaskRecurrence>>

    @Query("SELECT COUNT(*) FROM task")
    fun getCount(): Flow<Long>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert
    suspend fun insertPath(path: TaskPath)

    @Insert
    suspend fun insertCompletion(completion: TaskCompletion)

    @Insert
    suspend fun insertRecurrences(recurrences: Collection<TaskRecurrence>)

    @Insert
    suspend fun insertSkip(skip: TaskSkip)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun deleteRecurrences(recurrences: Collection<TaskRecurrence>)

    @Query(
        """INSERT INTO task_path
        (ancestor, descendant, depth)
        SELECT a.ancestor, d.descendant, a.depth + d.depth + 1
        FROM (SELECT ancestor, depth FROM task_path WHERE descendant = :parentId) a,
            (SELECT descendant, depth FROM task_path WHERE ancestor = :childId) d"""
    )
    suspend fun insertChain(parentId: Long, childId: Long)

    @Transaction
    suspend fun insert(form: NewTaskForm): Long {
        val id = insert(form.newTask)
        insertPath(TaskPath(ancestor = id, descendant = id, depth = 0))

        if (form.recurrences.isNotEmpty()) {
            insertRecurrences(form.recurrences.map { it.copy(taskId = id) })
        }

        if (form.parentTaskId != null) {
            insertChain(parentId = form.parentTaskId, childId = id)
        }

        return id
    }

    @Query(
        """SELECT EXISTS(
            SELECT *
            FROM task_path
            WHERE ancestor = :ancestor AND descendant = :descendant
        )"""
    )
    suspend fun hasPath(ancestor: Long, descendant: Long): Boolean

    @Query("SELECT descendant FROM task_path WHERE ancestor = :id AND depth = 1")
    suspend fun getChildIds(id: Long): List<Long>

    @Query(
        """UPDATE task_path
        SET depth = depth - 1
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :parentId)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor IN (:childIds))"""
    )
    suspend fun decrementDepth(parentId: Long, childIds: List<Long>)

    @Query("DELETE FROM task_path WHERE (ancestor = :taskId OR descendant = :taskId) AND depth > 0")
    suspend fun deletePathsByTaskId(taskId: Long)

    @Query(
        """DELETE FROM task_path
        WHERE ancestor IN (SELECT ancestor FROM task_path WHERE descendant = :id AND depth > 0)
            AND descendant IN (SELECT descendant FROM task_path WHERE ancestor = :id)"""
    )
    suspend fun deleteAncestors(id: Long)

    @Transaction
    suspend fun update(form: EditTaskForm) {
        update(form.updatedTask)

        if (form.recurrencesToRemove.isNotEmpty()) {
            deleteRecurrences(form.recurrencesToRemove)
        }

        if (form.recurrencesToAdd.isNotEmpty()) {
            insertRecurrences(form.recurrencesToAdd)
        }

        when (form.parentUpdateType) {
            is PathUpdateType.Keep -> {}
            is PathUpdateType.Remove -> {
                deleteAncestors(form.updatedTask.id)
            }

            is PathUpdateType.Replace -> {
                if (hasPath(
                        ancestor = form.updatedTask.id, descendant = form.parentUpdateType.id
                    )
                ) {
                    val existingParentId = getParent(form.updatedTask.id).first()?.id
                    val existingChildIds = getChildIds(form.updatedTask.id)
                    if (existingParentId != null && existingChildIds.isNotEmpty()) {
                        decrementDepth(parentId = existingParentId, childIds = existingChildIds)
                    }

                    deletePathsByTaskId(form.updatedTask.id)
                } else {
                    deleteAncestors(form.updatedTask.id)
                }

                insertChain(parentId = form.parentUpdateType.id, childId = form.updatedTask.id)
            }
        }
    }

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    suspend fun deleteWithPath(id: Long) {
        val parentId = getParent(id).first()?.id
        val childIds = getChildIds(id)
        deleteById(id)
        if (parentId != null && childIds.isNotEmpty()) {
            decrementDepth(parentId = parentId, childIds = childIds)
        }
    }

    @Query(
        """DELETE FROM task_completion
        WHERE task_id = :taskId 
            AND done_at IN (
                SELECT MAX(done_at) AS done_at
                FROM task_completion
                WHERE task_id = :taskId
            )"""
    )
    suspend fun deleteLatestTaskCompletionByTaskId(taskId: Long)
}
