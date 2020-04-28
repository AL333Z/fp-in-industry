# Video

[![12/2019 - FPinBO at LuogoComune (BO)](https://img.youtube.com/vi/x-3HqshXLps/maxresdefault.jpg)](https://youtu.be/x-3HqshXLps)

# Abstract

The Functional Programming paradigm has largely been emphasized in academia rather than commercial or industrial settings.
However, languages that support functional programming may show their best when used to actually deliver value to the business.

In this talk, You will see how a bunch of datatypes and techniques can help in implementing a pretty common business case and
You will then discover how there's no need to be a mathematician in order to be a functional programmer.

# How to run

- To bring up mongo and rabbit: `docker compose up`
- Export these env var
   - MONGO_USERNAME=root
   - MONGO_PASSWORD=example
   - RABBIT_USERNAME=guest
   - RABBIT_PASSWORD=guest
- run both applications: `sbt runMain api.OrderHistoryApp` and `sbt runMain projector.OrderHistoryProjectorApp`
- now you can push any message to the [queue](http://localhost:15672/#/queues/%2F/EventsFromOms), e.g.
```json
{
  "id": "001",
  "company": "ACME",
  "email": "asdf@asdf.com",
  "lines": [
    {
      "no": 1,
      "item": "jeans",
      "price": 100
    }
  ]
}
```
- and query the api [api](http://localhost/ACME/orders?email=%22asdf@asdf.com%22)

# How to run tests

`sbt test`
