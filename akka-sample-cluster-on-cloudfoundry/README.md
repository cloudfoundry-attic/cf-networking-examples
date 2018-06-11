This repo contains sample Akka Cluster app integrated with Service Discovery.

# How to run Akka Cluster on Cloud Foundry (CF)

Thanks to [this git repo](https://github.com/gtantachuco-pivotal/akka-sample-cluster-on-cloudfoundry) for the original version of this example, which uses Amalgam8.

This is a short guide to walk you through how to deploy and run an Akka Cluster-based application on [Cloud Foundry (CF)](https://cloudfoundry.org).

**Note:** Akka with no Remoting / Cluster can run on CF with no additional requirement. This article deals with cases when Remoting / Cluster features are used.

**Background:** [Akka Cluster](http://doc.akka.io/docs/akka/snapshot/scala/cluster-usage.html) is based on TCP communication or optionally can use UDP instead in Akka version >= 2.4.11.
CF's standard container-to-container (C2C) mechanism allows apps to talk to other apps via TCP or UDP; however, the ingress traffic to the entry point application supports only HTTP(S) and TCP.

In this guide we will use this CF C2C feature to show how to run an Akka Cluster that uses TCP.

## Prerequisites

The following instructions for this example assume the following:
- [This git repo](https://github.com/cloudfoundry/cf-networking-examples) is cloned to ~/workspace
- [The CF Networking Release git repo](https://github.com/cloudfoundry/cf-networking-release) is cloned to ~/workspace
- A Cloud Foundry with service discovery enabled; service discovery is part of the cf-networking-release

## Get ready to deploy apps to CF

- Login to CF:
```
cf login...
```
- Target the org and space where you want to deploy your apps:
```
cf target -o...
```

## Build the Akka application

You can deploy the Akka application by using your foundation's [java-buildpack](https://github.com/cloudfoundry/java-buildpack.git). Our sample application is inspired by the [akka-sample-cluster](https://github.com/akka/akka/tree/master/akka-samples/akka-sample-cluster-scala)).
It has backend nodes that calculate factorials upon receiving messages from frontend nodes. Frontend nodes also expose `GET <frontend-hostname>/info` that shows the number of jobs completed.

- Go to your local repo's folder
```
cd ~/workspace/cf-networking-examples/akka-sample-cluster-on-cloudfoundry/akka-sample-cluster/
```
- Compile and package both Akka backend and frontend components:
```
sbt backend:assembly # backend
sbt frontend:assembly # frontend
```

## Deploying Akka backend application

 **Note these commands assume your internal domain is `apps.internal`**

- Deploy, but do not start, the sample Akka backend app: with `-d apps.internal` and `--health-check-type none` options, since the backend app doesn't expose any HTTP ports:
```
cf push --no-start --health-check-type none akka-backend -p target/scala-2.11/akka-sample-backend.jar -b java_buildpack_offline -d apps.internal
```
- Map an internal route to the backend application:
```
cf map-route akka-backend apps.internal --hostname akka-backend
```
- Add this network policy, to allow the backend nodes to communicate:
```
cf add-network-policy akka-backend --destination-app akka-backend --port 2551 --protocol tcp
```
- Start the backend app:
```
cf start akka-backend
```
- Check the log to see that first node joined itself:
```
cf logs akka-backend
```
- **IMPORTANT:** To prevent cluster split, verify that the first node is running before scaling it.
- Scale backend to 2 instances:
```
cf scale akka-backend -i 2
```

## Deploying Akka frontend application

- Deploy, but don't start yet, the sample Akka frontend:
```
cf push akka-frontend --no-start -p target/scala-2.11/akka-sample-frontend.jar -b  java_buildpack_offline
```
- Add this network policy to allow the frontend app to communicate with the backend app via TCP on port 2551:
```
cf add-network-policy akka-frontend --destination-app akka-backend --port 2551 --protocol tcp
```
- Start the fronted app:
```
cf start akka-frontend
```
- In separate windows or terminal sessions, check logs from both frontend and backend to ensure all client/server and server-to-server communications are working fine:
```
cf logs akka-backend
cf logs akka-frontend
```
- Verify that it works:
```
curl akka-frontend.<YOUR_CF_DOMAIN>/info
```
- If all is working, it should show the number of completed jobs

## Summary

This guide shows the implementation of a successful PoC; hence, it requires more than that to have a production Akka Cluster on CF.
