## How to perform release

1. Create release branch or use your feature branch (pull request branch).
The point is to use another branch because *master* branch is protected and you can not simply push in it.

2. To prepare release run this command
```
mvn release:prepare
```

2.1 Answer questions and name your release.

2.2 Wait till maven prepares release (run tests, creates git tags, etc).

2.3 Successfull release should end with "BUILD SUCCESSFULL" message


---

If you want to skip tests during release run this:
```
mvn -Darguments='-Dmaven.test.skip=true' release:prepare
```


If you want to start over use this:
```
mvn -Darguments='-Dmaven.test.skip=true' release:clean
```