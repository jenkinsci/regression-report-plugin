Jenkins regression report plugin
================================
This plugin sends a mail if your test cases find regressions.

![built and tested on DEV@cloud](http://static-www.cloudbees.com/images/badges/BuiltOnDEV.png)


Scenario
--------
 * Importing CI into running project
   * Sometimes project have a lot of red tests
   * In this situation, finding regression (new red test cases) is little difficult
   * This plugin helps you to find them

Screen shot
-----------
![screen shot of configure](https://raw.github.com/jenkinsci/regression-report-plugin/master/screenshot.png)

Changelog
---------
### 1.0
- first release

### 1.1
- send mail to culprit [#1](https://github.com/jenkinsci/regression-report-plugin/issues/1)


Copyright and license
---------------------
Copyright 2012 Kengo TODA (eller86)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
