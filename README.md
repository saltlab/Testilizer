Testilizer plugin
==================

Testilizer is a tool which utilizes an existing Selenium test suite of a web application to generate new test cases with assertions. 

Paper
-----
A. Milani Fard, M. Mirzaaghaei, A. Mesbah, ["Leveraging Existing Tests in Automated Test Generation for Web Applications"](http://salt.ece.ubc.ca/publications/docs/testilizer-ase14.pdf), 29th IEEE/ACM International Conference on Automated Software Engineering (ASE), 2014 

Instructions
-----------------

Testilizer is built on top of Crawljax as a plugin. For the deployment, this local modified version of [Crawljax](https://github.com/aminmf/crawljax) should also be installed. The origianl test suites are located in crawljax/examples/src/main/java/com/crawljax/plugins/testilizer/casestudies/

Run it trough the Main class in crawljax/examples/src/main/java/com/crawljax/examples/TestilizerExample.java
