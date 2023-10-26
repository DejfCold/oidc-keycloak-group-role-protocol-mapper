package cz.dejfcold.keycloak.protocol

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.keycloak.models.*
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper
import org.keycloak.representations.AccessToken
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val GROUP_NAME = "group"
private const val GROUP_PARENT_NAME = "parentGroup"
private const val ROLE_NAME = "role"
private const val ROLE_NAME_SUB1 = "subcomp1"
private const val ROLE_NAME_SUB2 = "subcomp2"
private const val ROLE_NAME_COMPOSITE = "composite"

class GroupRoleProtocolMapperTest {

    private val mapper = GroupRoleProtocolMapper()

    @Test
    fun `getId should return the correct ID`() {
        val actual = mapper.id
        assertEquals(PROVIDER_ID, actual)
    }

    @Test
    fun `getHelpText should return non-blank help text`() {
        val actual = mapper.helpText
        assertTrue(actual.isNotBlank())
    }

    @Test
    fun `should contain expected config properties`() {
        val actual = mapper.configProperties
        assertTrue(actual.any { it.name == SPLIT_COMPOSITES })
        assertTrue(actual.any { it.name == INCLUDE_COMPOSITES })
        assertTrue(actual.any { it.name == OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN })
        assertTrue(actual.any { it.name == OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN })
    }

    @Test
    fun `should return the correct token mapper category`() {
        val actual = mapper.displayCategory
        assertEquals(AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY, actual)
    }

    @Test
    fun `should return the correct display type`() {
        val actual = mapper.displayType
        assertEquals(DISPLAY_TYPE, actual)
    }

    @Test
    fun `should set claim in access token with default configuration`() {
        val token = AccessToken()

        val mapperModel = ProtocolMapperModel()
        mapperModel.config = mapOf(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN to "true")

        val actual = mapper.transformAccessToken(
            token,
            mapperModel,
            null,
            createUserSession(),
            null
        )

        val actualClaim = actual.otherClaims[CLAIM_NAME] as Map<*, *>
        assertNotNull(actualClaim)

        val actualRoles = actualClaim["$GROUP_PARENT_NAME/$GROUP_NAME"] as Set<*>

        assertEquals(3, actualRoles.size)
        assertTrue(actualRoles.any { it == ROLE_NAME })
        assertTrue(actualRoles.any { it == ROLE_NAME_SUB1 })
        assertTrue(actualRoles.any { it == ROLE_NAME_SUB2 })

    }


    @Test
    fun `should set claim in access token with SPLIT_COMPOSITES set to false`() {
        val token = AccessToken()

        val mapperModel = ProtocolMapperModel()
        mapperModel.config = mapOf(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN to "true", SPLIT_COMPOSITES to "false")

        val actual = mapper.transformAccessToken(
            token,
            mapperModel,
            null,
            createUserSession(),
            null
        )

        val actualClaim = actual.otherClaims[CLAIM_NAME] as Map<*, *>
        assertNotNull(actualClaim)

        val actualRoles = actualClaim["$GROUP_PARENT_NAME/$GROUP_NAME"] as Set<*>

        assertEquals(2, actualRoles.size)
        assertTrue(actualRoles.any { it == ROLE_NAME })
        assertTrue(actualRoles.any { it == ROLE_NAME_COMPOSITE })
    }


    @Test
    fun `should set claim in access token with INCLUDE_COMPOSITES set to true`() {
        val token = AccessToken()

        val mapperModel = ProtocolMapperModel()
        mapperModel.config = mapOf(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN to "true", INCLUDE_COMPOSITES to "true")

        val actual = mapper.transformAccessToken(
            token,
            mapperModel,
            null,
            createUserSession(),
            null
        )

        val actualClaim = actual.otherClaims[CLAIM_NAME] as Map<*, *>
        assertNotNull(actualClaim)

        val actualRoles = actualClaim["$GROUP_PARENT_NAME/$GROUP_NAME"] as Set<*>

        assertEquals(4, actualRoles.size)
        assertTrue(actualRoles.any { it == ROLE_NAME })
        assertTrue(actualRoles.any { it == ROLE_NAME_COMPOSITE })
        assertTrue(actualRoles.any { it == ROLE_NAME_SUB1 })
        assertTrue(actualRoles.any { it == ROLE_NAME_SUB2 })
    }

    @Test
    fun `should throw GroupRoleException when token is null`() {
        val mapperModel = ProtocolMapperModel()
        mapperModel.config = mapOf(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN to "true")

        assertThrows<GroupRoleException> {
            mapper.transformAccessToken(
                null,
                mapperModel,
                null,
                null,
                null
            )
        }
    }

    private fun createUserSession(): UserSessionModel {
        val roleModelSubComp1 = createRoleModel(ROLE_NAME_SUB1)
        val roleModelSubComp2 = createRoleModel(ROLE_NAME_SUB2)
        val roleModelComposite = createRoleModel(ROLE_NAME_COMPOSITE, roleModelSubComp1, roleModelSubComp2)
        val roleModelSingular = createRoleModel(ROLE_NAME)
        val groupModelParent = createGroupModel(GROUP_PARENT_NAME)
        val groupModel = createGroupModel(GROUP_NAME, groupModelParent, roleModelSingular, roleModelComposite)
        val userModel = createUserModel(groupModel)

        return createUserSessionModel(userModel)
    }

    private fun createRoleModel(name: String, vararg composites: RoleModel): RoleModel {
        return mock<RoleModel> {
            on { this.name } doReturn name
            on { this.isComposite } doReturn composites.isNotEmpty()
            on { this.compositesStream } doReturn Stream.of(*composites)
        }
    }

    private fun createGroupModel(name: String, parent: GroupModel? = null, vararg composites: RoleModel): GroupModel {
        return mock<GroupModel> {
            on { this.roleMappingsStream } doReturn Stream.of(*composites)
            on { this.name } doReturn name
            on { this.parent } doReturn parent
        }
    }

    private fun createUserModel(vararg groups: GroupModel): UserModel {
        return mock<UserModel> {
            on { this.groupsStream } doReturn Stream.of(*groups)
        }
    }

    private fun createUserSessionModel(userModel: UserModel): UserSessionModel {
        return mock<UserSessionModel> {
            on { this.user } doReturn userModel
        }
    }
}