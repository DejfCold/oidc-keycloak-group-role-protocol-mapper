FROM quay.io/keycloak/keycloak:22.0.5
ADD build/libs/KeycloakGroupRoleProtocolMapper-1.0-SNAPSHOT.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
