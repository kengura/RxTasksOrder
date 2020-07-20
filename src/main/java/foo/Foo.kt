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
        val dispatcher = SequentialDispatcher(Schedulers.single())
        val count = 50

        val resultList = Collections.synchronizedList(ArrayList<Int>())
        val latch = CountDownLatch(count)

        for (i in 0..count) {
            when (i % 2) {
                0 -> Single.timer(Random.nextLong(0, 200), TimeUnit.MILLISECONDS)
                    .map { i }
                    .executeSequentially(dispatcher)
                    .subscribe({
                                   println(it)
                                   resultList.add(it)
                                   latch.countDown()
                               }, {})
                1 -> Single.fromCallable {
                    Thread.sleep(Random.nextLong(0, 200))
                    return@fromCallable i
                }
                    .executeSequentially(dispatcher, Schedulers.io())
                    .subscribe({
                                   println(it)
                                   resultList.add(it)
                                   latch.countDown()
                               }, {})
            }
        }

        latch.await()

        (0 until count).forEach {
            if (resultList[it] != it) {
                throw RuntimeException("Failed")
            }
        }
        println("Passed")
    }
}