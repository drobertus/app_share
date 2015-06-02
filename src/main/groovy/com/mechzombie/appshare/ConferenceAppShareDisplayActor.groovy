package com.mechzombie.appshare

import com.mechzombie.appshare.returned.ReturnedData
import com.mechzombie.appshare.state.ScreenState
import com.mechzombie.appshare.state.Window
import com.mechzombie.appshare.state.WindowParamEnum
import com.mechzombie.appshare.update.ScreenUpdate
import com.mechzombie.appshare.update.WindowDelta
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.actor.DefaultActor

@CompileStatic
@Slf4j
class ConferenceAppShareDisplayActor extends DefaultActor {

    /**
     * Screen state is a full working set of the data
     */
    protected ScreenState screenState = new ScreenState()


    /** A fast, read-only set of data that can be pulled without interference
     * to the working copy as windowData are made to it
    */
    private ScreenState readOnlyData = new ScreenState()



    private int revision = 0;

    @Override
    protected void act() {
        loop {
            react { ScreenUpdate inboundChanges ->
                addUpdates(inboundChanges)
            }
        }
    }

    /**
     * This is the main business logic of adding
     * new units of display and removing existing ones that match
     * the same location
     * @param changes
     * @return
     */
    private void addUpdates(ScreenUpdate screenChanges) {

        int newRev = revision + 1

        screenState.revision = newRev

        //see if the screen params need to be set
        if (screenChanges.screenHeight) {
            this.screenState.updateScreenParam(WindowParamEnum.screenHeight, screenChanges.screenHeight, newRev)
        }
        if (screenChanges.screenHeight) {
            this.screenState.updateScreenParam(WindowParamEnum.screenWidth, screenChanges.screenWidth, newRev)
        }

        for(WindowDelta aWin : screenChanges.windowDeltas) {
            //get the windows state
            Window stateWin = this.screenState.windows.get(aWin.id)
            //create a new one if necessary
            if(!stateWin) {
                stateWin = new Window(id: aWin.id)
                screenState.windows.put(stateWin.id, stateWin)
            }

            stateWin.updateState(newRev, aWin)

        }

        //TODO: update entire read only tree and update revision in one atomic operation
        println "Revision being set to $newRev"

        //get a copy of the data


        //set the readOnly version to be the copy with an atomic operation

        revision = newRev
    }

    /**
     * Use this for read-only access
     * @param lastRev
     * @return
     */
    ReturnedData getData(Integer lastRev = 0) {
        return getChangesSinceRev(lastRev)
    }


    private ReturnedData getChangesSinceRev(int rev) {

        log.info "getting windowData since $rev up to $revision"
        ReturnedData rd = new ReturnedData()
        rd.revision = this.screenState.revision

        screenState.getScreenParams().values().each {
            println "screen param = ${it.type}, value = ${it.value}, rev= ${it.revision}"
            if(it.revision > rev) {
                println "adding params ${it.type}"
                rd.screenParams.add(it)
            }
        }

        screenState.windows.values().each {
            if(it.revision > rev) {
                rd.windowData.add(it.getDelta(rev))
            }
        }
        return rd
    }
}
