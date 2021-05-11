#!/bin/bash

set -x

curl -H "Content-Type: application/json" -X GET -d "{\"from\":\"2011-10-05T14:48:00.000Z\",\"to\":\"2021-10-05T14:48:00.000Z\"}" http://localhost:8080/movie

curl -H "Content-Type: application/json" -X GET -d "{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\"}" http://localhost:8080/screening

curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"Tomasz\",\"userSurname\":\"Zieliński\",\"seats\":[{\"rowNo\":1,\"seatNo\":2}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation

curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"Tomasz\",\"userSurname\":\"Zieliński\",\"seats\":[{\"rowNo\":1,\"seatNo\":4}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation

curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"tomasz\",\"userSurname\":\"Zieliński\",\"seats\":[{\"rowNo\":1,\"seatNo\":5}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation

curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"Tomasz\",\"userSurname\":\"Ziel-Iński\",\"seats\":[{\"rowNo\":2,\"seatNo\":6}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation

curl -H "Content-Type: application/json" -X POST -d "{\"userName\":\"Tomasz\",\"userSurname\":\"Ziel-iński\",\"seats\":[{\"rowNo\":1,\"seatNo\":6}],\"screening\":{\"title\":\"Shrek\",\"start\":\"2021-06-01T18:30:00.000Z\",\"room\":1},\"tickets\":{\"adultTickets\":1,\"studentTickets\":0,\"childTickets\":0}}" http://localhost:8080/reservation
