package cz.dejfcold.keycloak.protocol

import org.keycloak.models.*
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.representations.IDToken
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.asSequence

internal const val DISPLAY_TYPE = "Group to Role"
internal const val PROVIDER_ID = "oidc-group-role-mapper"
internal const val SPLIT_COMPOSITES = "cz.dejfcold.group-role.split-composites"
internal const val INCLUDE_COMPOSITES = "cz.dejfcold.group-role.include-composites"
internal const val CLAIM_NAME = "group-roles"

private fun <T> Stream<T>.toSet() = this.collect(Collectors.toSet())

class GroupRoleProtocolMapper : OIDCAccessTokenMapper, OIDCIDTokenMapper, AbstractOIDCProtocolMapper() {

    private companion object {
        private var _configProperties = mutableListOf<ProviderConfigProperty>()

        init {
            OIDCAttributeMapperHelper.addIncludeInTokensConfig(
                _configProperties, GroupRoleProtocolMapper::class.java
            )

            _configProperties += ProviderConfigProperty().apply {
                name = SPLIT_COMPOSITES
                label = "Split composite roles"
                helpText = "Recursively split composite roles."
                defaultValue = true
                type = ProviderConfigProperty.BOOLEAN_TYPE
            }

            _configProperties += ProviderConfigProperty().apply {
                name = INCLUDE_COMPOSITES
                label = "Include composite roles when splitting"
                helpText = "If the 'Split composite roles' is set, also keep the split composite roles."
                defaultValue = false
                type = ProviderConfigProperty.BOOLEAN_TYPE
            }

        }
    }

    override fun getId(): String {
        return PROVIDER_ID
    }

    override fun getHelpText(): String {
        return "Creates a map in the token where the keys are groups to which the user belongs and values are lists of roles inherited from said groups"
    }

    override fun getConfigProperties(): MutableList<ProviderConfigProperty> {
        return _configProperties
    }

    override fun getDisplayCategory(): String {
        return TOKEN_MAPPER_CATEGORY
    }

    override fun getDisplayType(): String {
        return DISPLAY_TYPE
    }

    override fun setClaim(
        token: IDToken?,
        mappingModel: ProtocolMapperModel?,
        userSession: UserSessionModel?,
        keycloakSession: KeycloakSession?,
        clientSessionCtx: ClientSessionContext?
    ) {
        if (token == null) {
            throw GroupRoleException("No token to update!")
        }

        val config = mappingModel?.config
        val splitComposites = config?.get(SPLIT_COMPOSITES)?.toBoolean() ?: true
        val includeComposites = config?.get(INCLUDE_COMPOSITES)?.toBoolean() ?: false

        val groupSequence = userSession?.user?.groupsStream?.asSequence() ?: emptySequence()

        val groupsToRoles = groupSequence.associateWith { extractRoles(it, splitComposites, includeComposites) }
            .mapKeys { getFullGroupName(it.key) }

        token.otherClaims += CLAIM_NAME to groupsToRoles
    }

    private fun extractRoles(
        group: GroupModel,
        splitComposites: Boolean,
        includeComposites: Boolean
    ): MutableSet<String> {
        if (!splitComposites) {
            return group.roleMappingsStream.map { it.name }.toSet()
        }

        return group.roleMappingsStream.flatMap { splitCompositeRole(it, includeComposites) }.toSet()
    }

    private fun getFullGroupName(group: GroupModel): String {
        val reversePath = mutableListOf(group.name)
        var curGroup = group
        while (curGroup.parent != null) {
            curGroup = curGroup.parent
            reversePath += curGroup.name
        }
        return reversePath.reversed().joinToString("/")
    }

    private fun splitCompositeRole(role: RoleModel, includeComposites: Boolean): Stream<String> {
        return if (role.isComposite) {
            var roles = role.compositesStream.flatMap { splitCompositeRole(it, includeComposites) }
            if (includeComposites) {
                roles = Stream.concat(roles, Stream.of(role.name))
            }
            return roles
        } else {
            Stream.of(role.name)
        }
    }
}