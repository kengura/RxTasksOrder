package foo

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

typealias TaskKey = String

fun Completable.observeChain(
    dispatcher: RxTasksChainDispatcher,
    key: TaskKey = UUID.randomUUID().toString()
): Completable {
    return dispatcher.add(this.toObservable(), key)
        .ignoreElements()
}

inline fun <reified T : Any> Single<T>.observeChain(
    dispatcher: RxTasksChainDispatcher,
    key: TaskKey = UUID.randomUUID().toString()
): Single<T> {
    return dispatcher.add(this.toObservable(), key)
        .firstOrError()
}

class RxTasksChainDispatcher() {

    @Volatile private var latestTaskKey: TaskKey = UUID.randomUUID().toString()
    private val completedTasksPublisher = BehaviorSubject.createDefault<TaskKey>(latestTaskKey)

    @Synchronized
    fun <T : Any> add(task: Observable<T>, key: TaskKey): Observable<T> {
        val previousTaskKey = latestTaskKey
        latestTaskKey = key
        return task
            .flatMap { result ->
                completedTasksPublisher.filter { it == previousTaskKey }
                    .observeOn(Schedulers.single())
                    .map { result }
            }
            .doFinally { completedTasksPublisher.onNext(key) }
    }
}
