package foo

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.random.Random

object Foo {

    @JvmStatic
    fun main(args: Array<String>) {
        val dispatcher = RxTasksChainDispatcher()
        val count = 100

        val resultList = Collections.synchronizedList(ArrayList<Int>())
        val latch = CountDownLatch(count)

        for (i in 0..count) {
            when (i % 2) {
                0 -> Single.timer(Random.nextLong(0, 100), TimeUnit.MILLISECONDS)
                    .map { i }
                    .observeChain(dispatcher)
                    .subscribe({
                                   resultList.add(it)
                                   latch.countDown()
                               }, {})
                1 -> Single.fromCallable {
                    Thread.sleep(Random.nextLong(0, 100))
                    return@fromCallable i
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .observeChain(dispatcher)
                    .subscribe({
                                   resultList.add(it)
                                   latch.countDown()
                               }, {})
            }
        }

        latch.await()

        if ((0..count).toList() == resultList) {
            println("Passed")
        } else {
            throw RuntimeException("Failed")
        }
    }
}