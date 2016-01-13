# rxjava maven docker toy example


#### prerequisites

working docker/docker-machine installation


#### start a mongo db in a docker container for integration testing

`mvn docker:start`


#### stop with

`CTRL+C`


#### run all tests (starts and stops the mongo-docker container)

`mvn verify``


#### clean up (in case of unexpected docker problems)

`docker stop $(docker ps -aq) && docker rm $(docker ps -aq)`