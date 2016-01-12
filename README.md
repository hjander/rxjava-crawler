# rxjava maven docker toy example


#### prerequisites

working docker/docker-machine installation


#### start with

`mvn integration-test`

#### stop with

`CTRL+C`

#### clean up (the plugin doesnt stop containers automatically)

`docker stop $(docker ps -aq) && docker rm $(docker ps -aq)`