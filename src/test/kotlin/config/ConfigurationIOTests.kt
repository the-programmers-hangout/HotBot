package config

import me.aberrantfox.aegeus.businessobjects.produceConfigOrFail
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.Thread.sleep

class ConfigurationIOTests {
    val fileLocation = "testLocation.json"
    val dummyMethod = javaClass.methods[0]

    @Before @After fun setup() {
        val file = File(fileLocation)
        file.delete()

        //give the os some time to release any locks to the file
        sleep(100)
    }

    @Test fun configShouldBeCreatedIfNotExists() {
        val mockMap = mutableMapOf("test" to dummyMethod)
        val config = produceConfigOrFail(mockMap, fileLocation)

        assertNull(config)
    }

    @Test fun configShouldNotBeNullAfterCreation() {
        val mockMap = mutableMapOf("test" to dummyMethod)
        val config = produceConfigOrFail(mockMap, fileLocation)

        if(config != null) {
            fail("Config did not exist, but returned a non-null value")
            return
        }

        val actualConfig = produceConfigOrFail(mockMap, fileLocation)
        assertNotNull(actualConfig)
    }
}