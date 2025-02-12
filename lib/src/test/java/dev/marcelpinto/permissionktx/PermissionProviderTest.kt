package dev.marcelpinto.permissionktx

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Test

class PermissionProviderTest {


    private val fakeChecker = object : PermissionChecker {
        override fun getStatus(type: Permission) = PermissionStatus.Granted(Permission("any"))
    }

    private val dummyObserver = object : PermissionObserver {
        override fun getStatusFlow(type: Permission) = emptyFlow<PermissionStatus>()
        override fun refreshStatus() {}
    }

    @Test
    fun `test isInitialized when PermissionProvider is initialized`() {
        assertThat(PermissionProvider.isInitialized()).isFalse()
        PermissionProvider.init(fakeChecker, dummyObserver)
        assertThat(PermissionProvider.isInitialized()).isTrue()
    }
}
