# Hyperwave

An experiment at a simple REST service that looks sorta like Twitter. It was going to be more
involved, but real distributed systems turn out to be all kinds of difficult so this turned into an
experiment in how far can I get on a single $5 DigitalOcean instance. Turns out pretty far.

Internally, the message "stream" (sequence) is built as a linked list of REDIS keys.

- `hyperwave:$id` as a key maps to the key of the previous message
- `hyperwave:$id:body` as a key maps to the encoded body of the message
- `hyperwave:head` is a magic key being being the ID of the most recent post

Posts thus form a linked list, with a new post being a transaction on `hyperwave:head` to insert
what is essentially a new cons cell.

## HTTP/JS "client"

The HTTP polling client achieves a degree of eventual consistency by pulling down the last 64 posts,
inserting any posts which aren't already in the DOM, and if the oldest post in the most recent 64
was not in the DOM then it chain loads all other missing posts by walking the history linked list
backwards until it reaches the first post already in the DOM.

Polling occurs on a 1s clock tick minimum, and backoff is used to slow refreshes down to once per
20s. New posts resets the backoff timer to 1s.

## REST API

```
GET /api/v0/stats
  Returns service load information

GET /api/v0/p
  Returns {status "OK", body : [POST_OBJ ...]}
  Being the most recent 64 posts in order.

POST /api/v0/p
  supported params: author=, body=, reply_to=
  If successful, returns a 200 with the inserted document by ID
  Otherwise returns a 503 with slowdown information and an error
  May return a 500 with validation errors

GET /api/v0/p/:id
  Returns a 200 with {status "OK", body {ar POST_OBJ dr PREV_POST_ID}}
  Otherwise returns a 404 if there is no such document
```

## Legal

Copyright © 2016 Reid 'arrdem' McKenzie

Distributed under the WTFPBL. If you use this, you're doing it wrong.
