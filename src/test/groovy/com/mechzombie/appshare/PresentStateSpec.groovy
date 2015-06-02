package com.mechzombie.appshare

import com.mechzombie.appshare.returned.ReturnedData
import com.mechzombie.appshare.state.DisplayUnit
import com.mechzombie.appshare.state.WindowParamEnum
import com.mechzombie.appshare.update.ScreenUpdate
import com.mechzombie.appshare.update.WindowDelta
import spock.lang.Specification

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class PresentStateSpec extends Specification {

    ConferenceAppShareDisplayActor screen

    public void setup() {
        screen = new ConferenceAppShareDisplayActor()
        screen.start()
    }

    def "check Initialized State"() {
        expect:
        assertTrue screen.screenState.windows.size() == 0
        assertEquals 0, screen.screenState.revision
    }

    def "sent_check_return_add_more_check_responses" () {

        setup:
        def blockDim = 50
        def screenHeight = 1024
        def screenWidth = 768
        def windowId = 4
        def blockCountY = 3
        def blockCountX = 6
        def expectedBlockCount = blockCountY * blockCountX
        def height = (blockDim * blockCountY)
        def width = (blockDim * blockCountX)
        def winX = 54
        def winY = 27
        def winZ = 7

        ScreenUpdate su = new ScreenUpdate()

        WindowDelta sharedWin1 = new WindowDelta(id: windowId)
        Map<String, Integer> params = sharedWin1.windowParams
        params.put('x', winX)
        params.put('y', winY)
        params.put('z', winZ)
        params.put('height', height)
        params.put('width', width)

        (width.intdiv(blockDim)).times { x ->
            (height.intdiv(blockDim)).times { y ->
                //we don't actaully care about the size of the blocks in the display
                sharedWin1.updates.add(new DisplayUnit(x: x, y: y, payload: " units at $x $y"))
            }
        }
        su.windowDeltas.add(sharedWin1)
        su.screenHeight = screenHeight
        su.screenWidth = screenWidth

        when: "we send the inital scren state"
        screen.send(su)
        sleep 300
        def windowInState = screen.screenState.windows.get(windowId)

        then: "the state on the server should match it"


        assertEquals "initial window width not correct ",width, windowInState.params.get(WindowParamEnum.width).value
        assertEquals "initial window height not correct ",height, windowInState.params.get(WindowParamEnum.height).value
        assertEquals "initial window X not correct ",winX, windowInState.params.get(WindowParamEnum.x).value
        assertEquals "initial window Y not correct ",winY, windowInState.params.get(WindowParamEnum.y).value
        assertEquals "initial window Z not correct ",winZ, windowInState.params.get(WindowParamEnum.z).value

        assertEquals "Windows count sent should equal present", su.windowDeltas.size(),
            screen.screenState.windows.size()

        when: "we get the  screen state for a client"
        ReturnedData firstView = screen.getData()

        int returnedScreenHeight
        int returnedScreenWidth
        firstView.screenParams.each {
            println "returned window param = $it"
            if(it.type.equals(WindowParamEnum.screenHeight)){
                returnedScreenHeight = it.value
            }
            else if(it.type.equals(WindowParamEnum.screenWidth)){
                returnedScreenWidth = it.value
            }
        }

        then: "the new clients screen data should match that on the server"

        assertEquals "screen width not expected", screenWidth, returnedScreenWidth
        assertEquals "screen height not expected", screenHeight, returnedScreenHeight
        assertEquals "We have made only 1 update, so revision should be 1", 1, firstView.revision

        assertEquals "Number of windows sent should equal returned",
            screen.screenState.windows.size(), firstView.windowData.size()

        and: "the details of the window should match"
            def win1= screen.screenState.windows.get(windowId)
            def returnedWin = firstView.windowData.get(0)

            assertNotNull(win1)

            assertEquals win1.id, returnedWin.id
            assertEquals 1, firstView.revision


        assertEquals "Number of blocks for window sent should equal returned",
            win1.display.size(), returnedWin.contents.size()

        when: "an update is sent to the server"
        //now send a small update
        def screenUpdate = new ScreenUpdate()

        WindowDelta delta = new WindowDelta(id: windowId)
        delta.updates.add( new DisplayUnit(x: 0, y:1, payload: "update at 0-1"))
        delta.updates.add( new DisplayUnit(x: 5, y:0, payload: "update at 5-0"))
        screenUpdate.windowDeltas.add(delta)

        screen.send(screenUpdate)
        sleep 50
        returnedScreenHeight
        returnedScreenWidth
        firstView.screenParams.each {
            if(it.type.equals(WindowParamEnum.screenHeight)){
                returnedScreenHeight = it.value
            }
            if(it.type.equals(WindowParamEnum.screenWidth)){
                returnedScreenWidth = it.value
            }
        }

        then: "the server's screen meta-data should be left unchanged"

        assertEquals "screen width not expected", screenWidth, returnedScreenWidth
        assertEquals "screen height not expected", screenHeight, returnedScreenHeight
        assertEquals "update at 5-0", screen.screenState.windows.get(4).getDisplay().get('5-0').payload

        and: "the server's revision id should be updated"

        assertEquals 2, screen.screenState.revision

        when: "the client gets the revision after the update"
        //now set see that we get back this as a time based update
        def updateResponse = screen.getData(firstView.revision)

        then: "only the updates should be returned"
        assertEquals 2, updateResponse.revision
        assertEquals "There should only be one window in the update", 1, updateResponse.windowData.size()
        assertEquals "There should only be one window in the update", delta.id, updateResponse.windowData.get(0).id
        assertEquals "There should only be two update units in the window update",
            2, updateResponse.windowData.get(0).contents.size()
        assertTrue updateResponse.screenParams.isEmpty()
        assertTrue updateResponse.windowData.get(0).params.isEmpty()


        when: "a new client get's the screen state"

        def newClientData = this.screen.getData()

        then: "the full screen state should be returned"
        assertEquals 2, newClientData.revision
        assertEquals "There should only be one window in the response", 1, newClientData.windowData.size()
        assertEquals "The window id should be set", windowId, newClientData.windowData.get(0).id
        assertEquals "There should only be two update units in the window update",
            expectedBlockCount, newClientData.windowData.get(0).contents.size()
        assertEquals 2, newClientData.screenParams.size()
        assertEquals 5, newClientData.windowData.get(0).params.size()

    }
}
