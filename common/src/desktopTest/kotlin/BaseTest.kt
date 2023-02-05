import com.darkrockstudios.apps.hammer.common.dependencyinjection.DISPATCHER_DEFAULT
import com.darkrockstudios.apps.hammer.common.dependencyinjection.DISPATCHER_IO
import com.darkrockstudios.apps.hammer.common.dependencyinjection.DISPATCHER_MAIN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
open class BaseTest : KoinTest {

    protected val scope = TestScope()

    @Before
    open fun setup() {
        Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))
    }

    @After
    open fun tearDown() {
        stopKoin()
    }

    fun setupKoin(vararg modules: Module) {
        val scheduler = scope.testScheduler
        GlobalContext.startKoin {
            modules(
                module {
                    single<CoroutineContext>(named(DISPATCHER_DEFAULT)) {
                        StandardTestDispatcher(
                            scheduler,
                            name = "Default dispatcher"
                        )
                    }
                    single<CoroutineContext>(named(DISPATCHER_IO)) {
                        StandardTestDispatcher(
                            scheduler,
                            name = "IO dispatcher"
                        )
                    }
                    single<CoroutineContext>(named(DISPATCHER_MAIN)) {
                        StandardTestDispatcher(
                            scheduler,
                            name = "Main dispatcher"
                        )
                    }
                },
                *modules
            )
        }
    }
}