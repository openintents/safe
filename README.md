# OI Safe

Requires OI Distribution. A quick way to build would be:

```bash
$ mkdir oisafe
$ cd oisafe
$ git clone https://github.com/openintents/safe.git
$ git clone https://github.com/openintents/distribution.git
$ cd distribution/DistributionLibrary/
$ cp template\ of\ local.properties local.properties
$ vi local.properties
$ ant debug
$ cd ../../safe/Safe
$ cp template\ of\ local.properties local.properties
$ vi local.properties
$ ant debug
```
