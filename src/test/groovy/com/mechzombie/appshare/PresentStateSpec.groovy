package com.mechzombie.appshare

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class PresentStateSpec {

    ConferenceAppShareDisplayActor screen
    int width = 10
    int height = 2

    @Before
    public void setup() {
        screen = new ConferenceAppShareDisplayActor(height, width)
        screen.start()
    }

    @Test
    public void checkInitializedState() {
        assertTrue screen.screen.size() == 0
    }

    @Test
    public void sent_check_return_add_more_check_responses() {
        def firstShowing = []
        width.times { x ->
            height.times { y ->
                firstShowing.add(new DisplayUnit(x: x, y: y, payload: " units at $x $y"))
            }
        }
        screen.send(firstShowing)

        long endTime = System.currentTimeMillis() + 2500
        while((width * height) != screen.screen.size() && System.currentTimeMillis() < endTime) {
            sleep(35)
        }
        println ("took ${System.currentTimeMillis() - (endTime - 2500)}ms to load inital data")
        assertEquals "initial screen  size not correct ",(width * height), screen.screen.size()

        ReturnedData firstView = screen.getData()

        assertEquals "first screen view should be identical ",(width * height),firstView.returnedData.size()

        //now send a small update
        def windowUpdate = []
        windowUpdate.add(new DisplayUnit(x: 0, y:1, payload: "update at 0-1"))
        windowUpdate.add(new DisplayUnit(x: 5, y:0, payload: "update at 5-0"))

        screen.send(windowUpdate)

        assertEquals "updated screen size should be identical ",(width * height),screen.screen.size()

        //now set see that we get back this as a time based update
        def updateResponse = screen.getData(firstView.returnTime)
        assertEquals (windowUpdate.size(), updateResponse.returnedData.size())
        assertEquals "updated screen size should be identical ",(width * height),screen.screen.size()


        //now lets get an update from a time before the main set
        def backuptime = firstView.returnTime - 1000
        def timeMachine = screen.getChangesSinceTime(backuptime)
        println "returned with ${timeMachine.returnedData.size()} records after going back in time before the initial call"

        assertEquals ("Expecting " + windowUpdate.size() + " records got " +
            timeMachine.returnedData.size(), timeMachine.returnedData.size(), screen.screen.size())

    }
}
