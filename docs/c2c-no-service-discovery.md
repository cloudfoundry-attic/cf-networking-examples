# Cats and Dogs without Service Discovery

This example demonstrates communication (HTTP and UDP) between a frontend and a backend application over the container network.

We're assuming that you've [deployed to BOSH lite](https://github.com/cloudfoundry/cf-deployment).
If you've [deployed to AWS](https://github.com/cloudfoundry/cf-deployment) or another environment,
substitute `bosh-lite.com` below with the domain name of your installation.

To configure policies you use the [CF CLI](https://github.com/cloudfoundry/cli).


## Frontend
The frontend serves a form at `http://frontend.bosh-lite.com/`.

The frontend allows you to test out container network communication via two methods:

- connect to the backend via HTTP
- connect to the backend via UDP

In either case, the response from the backend to the frontend will be rendered as a web page.


### Deploying
```
cd frontend
cf api api.bosh-lite.com --skip-ssl-validation
cf auth admin admin
cf push frontend
```


## Backend
The backend displays its platform-generated internal hostname and container network IP if you visit `http://backend.bosh-lite.com/`.

Note: the platform-generated internal hostname will only work if cf was deployed with service discovery.

The backend serves pictures of cats on the TCP ports specified in the environment variable `CATS_PORTS`,
and responds to simple text messages on the UDP ports specified in the environment variable `UDP_PORTS`.


### Deploying
```
cd backend
cf push backend --no-start
cf set-env backend CATS_PORTS "7007,7008"
cf set-env backend UDP_PORTS "9003,9004"
cf start backend
```


## Usage

After both frontend and backend apps have been deployed, you can visit `http://backend.bosh-lite.com/`
in a browser. You should see something like:

```
My overlay IP is: 10.255.76.2

I'm serving cats on TCP ports 7007,7008

I'm also serving a UDP echo server on UDP ports 9003,9004
```

If you were to visit `http://frontend.bosh-lite.com`, you should see something like:

```
Frontend Sample App

HTTP Test
Backend HTTP URL: [....] [ Submit ]

UDP Test
Backend UDP Server Address: [....]
Message: [....] [ Submit ]
```


### Usage with HTTP

In the frontend app, in the `Backend HTTP URL` field, enter the backend's internal hostname and a cats port (`<internal-hostname>.apps.internal:7007`).
Hit submit.
If service discovery is not enabled or if you haven't created an internal route, you may use the backend's overlay IP in place of the hostname.

You will see an error message saying the connection is refused. This is because the two apps have not been
configured to allow connections from the frontend to the backend.

If you see an error message saying `no such host`, service discovery is incorrectly configured.

Now allow access:

```
cf add-network-policy frontend --destination-app backend --port 7007 --protocol tcp
```

Now if you try again from the frontend:

```
[GIF OF CAT]
Hello from the backend, port: 7007
```


### Usage with UDP

In the frontend apps, in `Backend UDP Server Address` field, enter the backend's internal hostname and UDP port
(`<internal-hostname>.apps.internal:9003`) and a message. Hit submit.
If service discovery is not enabled or if you haven't created an internal route, you may use the backend's overlay IP in place of the hostname.

You will see an error message. This is because the two apps have not been
configured to allow connections from the frontend to the backend on this port.

Now allow access:

```
cf add-network-policy frontend --destination-app backend --port 9003 --protocol udp
```

Now if you try again from the frontend:

```
You sent the message: hello world

Backend UDP server replied: HELLO WORLD
```
