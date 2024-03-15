 # Pipeline Overview
![pipeline](./data/pipeline.png)

# Confusions
1. UserAgent (UA): I took "userId" from json payload as UA if that is not correct then logic is there to extract UA from http request and plug that into the logic
2. Hourly Stats: Timestamp in json payload is not a required param thus i attached current timestamp which could also be changes if needed

# How to Run
1. Docker installation is prerequisite
2. Run ```docker-compose up -d```, maksure that all the containers are running (wait around 2 to 5 mins) ![container](./data/container.png)
3. check ```http://localhost:8083/connectors``` if it returns ```[
"jdbc"
]``` otherwise rerun ```docker-compose down -v connect``` and  ```docker-compose up -d connect``` and wait again for a min
4. Nginx load balancer is used to handle bursts of requests so please increase 'api' replicas to desired number (in docker compose file), currently it is set to 1 which can handle around 10k requests per 5 seconds but eventully it depends on tester hardware resources
5. Post json payload to ```http://localhost:4000/api/log_event``` to make a request
   - i have tried covering all the use cases and return some useful error too
   - following request return 'Block UA' error message but this request is counted for hourly stats as explained in task: ```{
                        "customerID": 1,
                        "tagID": 2,
                        "userID": "A6-Indexer",
                        "remoteIP": "1.1.1.1",
                        "timestamp": 1500000000
                    }```
6. api to get stats (which accepts either date param formatted as 'dd.MM.yyyy' or day params which is 'day of the year')
   - http://localhost:4000/api/stats?customerId=1&date=13.03.2024   or
   - http://localhost:4000/api/stats?customerId=4&day=73 ```{
            "customerId": 4,
            "day": 74,
            "date": "2024-03-14T00:00:00.000+00:00",
            "customerDayStats": {
                "request_count": 4,
                "invalid_count": 0
            },
            "dayStats": {
                "request_count": 4,
                "invalid_count": 0
            }
        }```

# Improvements:
1. there are other ways to handle this task too, i do have couple of other solutions in mind, let's discuss that later in call
2. api request handling could be improved but i focused on overall pipeline rather writing perfect api (which could be improved later)