Build 1
Status: SUCCESS
Authors: empty (Jenkins does not have change set for the first build)
Tests:
	pkg.AppTest.testApp1: Passed
	pkg.AppTest.testApp2: Passed
	pkg.AppTest.testApp3: Skipped

Build 2
Status: SUCCESS
Authors: developer2
Tests:
	pkg.AppTest.testApp1: Passed
	pkg.AppTest.testApp2: Passed
	pkg.AppTest.testApp3: Passed

Build 3
Status: SUCCESS
Authors: developer1, developer2 
Tests:
	pkg.AppTest.testApp1: Passed
	pkg.AppTest.testApp2: Passed
	pkg.AppTest.testApp3: Passed
	pkg.AppTest.testApp4: Passed

Build 4
Status: UNSTABLE
Authors: developer2
Tests:
	pkg.AppTest.testApp1: Passed
	pkg.AppTest.testApp2: Passed
	pkg.AppTest.testApp3: Passed
	pkg.AppTest.testApp4: Passed
	pkg.AppTest.testApp5: Failed

Build 5
Status: UNSTABLE
Authors: developer1
Tests:
	pkg.AppTest.testApp1: Passed
	pkg.AppTest.testApp2: Failed
	pkg.AppTest.testApp3: Failed
	pkg.AppTest.testApp5: Passed
