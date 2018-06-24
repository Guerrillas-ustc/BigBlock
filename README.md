# BigBlock
A centralized block-chain platform for trading data.


## How to use
System requirement: JDK-8, docker and docker-compose

1. Build
```
./gradlew installdist
```
2. Start Cassandra database
```
docker-compose up
```
3. Start Http server
```
./server/build/install/server/bin/server
```
4. (Optional) Start the demo
```
./build/install/BigBlock/bin/BigBlock
```
