FROM denvazh/gatling:3.2.1

COPY src/gatling/ /opt/gatling/user-files/

COPY docker/entrypoint.sh /opt/gatling/
ENTRYPOINT ["/opt/gatling/entrypoint.sh"]