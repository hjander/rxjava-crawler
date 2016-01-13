# crawler with rxjava maven docker as toy example


#### prerequisites

working docker/docker-machine installation


#### start a mongo db in a docker container for integration testing

`mvn docker:start`


#### start the crawler

run `net.example.ReactiveCrawler.main()`


#### see running containers

run `docker ps``


#### read the mongo logs

`docker logs --tail="all" $MONGO_CONTAINER_ID`


#### run all tests (starts and stops the mongo-docker container)

`mvn verify``


#### clean up (in case of unexpected docker problems)

`docker stop $(docker ps -aq) && docker rm $(docker ps -aq)`