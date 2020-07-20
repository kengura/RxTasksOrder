package foo

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.atomic.AtomicLong

fun Completable.executeSequentially(
    dispatcher: SequentialDispatcher,
    executeScheduler: Scheduler? = null
): Completable {
    return dispatcher.execute(this.toObservable(), executeScheduler)
        .ignoreElements()
}

inline fun <reified T : Any> Single<T>.executeSequentially(
    dispatcher: SequentialDispatcher,
    executeScheduler: Scheduler? = null
): Single<T> {
    return dispatcher.execute(this.toObservable(), executeScheduler)
        .firstOrError()
}

/**
 * Диспетчер задач, который запускает их поочередно одну за другой.
 *
 * [sequentialScheduler] - шедулер поддерживающий строгую очередность выполнения, так же результаты выполнения будут
 * возвращены на этом шедулере. По умолчанию используется [Schedulers.single]

 */
class SequentialDispatcher(private val sequentialScheduler: Scheduler = Schedulers.single()) {

    private val latestTaskKey = AtomicLong(0)
    private val completedTasksPublisher = BehaviorSubject.createDefault<Long>(latestTaskKey.get())

    /**
     * Добавляет задачу [task] в очередь исполнения, результат выполнения вернется на [sequentialScheduler].
     * [executeScheduler] - scheduler для выполнения задачи. Если null, тогда используется либо дефолтный
     * шедулер для задачи (например [Schedulers.computation] для [Completable.timer]), либо выполнение будет происходить
     * на [sequentialScheduler].
     */
    @Synchronized
    fun <T : Any> execute(task: Observable<T>, executeScheduler: Scheduler?): Observable<T> {
        val previousTaskKey = latestTaskKey.get()
        val key = latestTaskKey.incrementAndGet()
        return completedTasksPublisher
            .filter { it == previousTaskKey }
            .let { if (executeScheduler != null) it.observeOn(executeScheduler) else it }
            .flatMap { task }
            .observeOn(sequentialScheduler)
            .doFinally { completedTasksPublisher.onNext(key) }
    }
}
