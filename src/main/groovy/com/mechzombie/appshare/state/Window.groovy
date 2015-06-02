package com.mechzombie.appshare.state

import com.mechzombie.appshare.returned.WindowUpdate
import com.mechzombie.appshare.update.WindowDelta
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

@CompileStatic
class Window {

    int revision
    int id

    //TODO: need to handle revisions of Window meta data
    Map<WindowParamEnum, WindowParameter> params = new HashMap<WindowParamEnum, WindowParameter>()

    //stores revision info for window meta data

    //fast access to display to make copies and get revision
    ConcurrentHashMap<String, DisplayUnit> display = new ConcurrentHashMap<String, DisplayUnit>()

    //stores revision info for window display data
    ConcurrentSkipListMap<Integer, Map<String, DisplayUnit>> receivedUnits =
        new ConcurrentSkipListMap<Integer, Map<String, DisplayUnit>>()

    //get all the revisions since the received on
    WindowUpdate getDelta(int lastRevision) {
        println "getting window $id delta from rev $lastRevision"
        if (lastRevision >= this.revision) {
            println "returning since request is for later rev"
            return null
        }

        WindowUpdate winRev = new WindowUpdate(id: this.id)
        println("Window $id has ${params.size()} params")
        //get windowData in the window meta data since this last revision
        this.params.values().each {
            println("checking param ${it.type.toString()} at rev ${it.revision}")
            if(it.revision > lastRevision || lastRevision ==0) {
                println "adding param ${it.type} to windows $id params"
                winRev.params.put(it.type, it.value)
            }
        }

        //get the screen updates since the lastRevision (passed in)
        NavigableSet<Integer> kets = receivedUnits.descendingKeySet()// .navigableKeySet()
        Iterator<Integer> iter = kets.iterator()
        int curr
        while(iter.hasNext()) {
            curr = iter.next()
            println "curr rev=$curr"
            //this is dependent on the fact we have a descending navigable map
            if(curr <= lastRevision) {
                break
            }

            def revisionContents = receivedUnits.get(curr).values()
            println "adding contents of $curr to windows $id display with ${revisionContents.size()} units"
            winRev.contents.addAll(revisionContents)

        }
        return winRev
    }

    void updateState(int newRevision, WindowDelta windowDelta) {
        println "updating window ${this.id}"
        //set the revision number on it

        //set or update the params
        if (windowDelta.windowParams) {
            windowDelta.windowParams.entrySet().each {
                WindowParamEnum param = WindowParamEnum.valueOf(it.key)
                params.put(param, new WindowParameter(type: param, value: it.value, revision: revision) )
            }
        }

        //set or update the content
        Map<String, DisplayUnit> revisionIds = [:]
        windowDelta.updates.each {
            String id = it.x + '-' + it.y
            it.revision = newRevision
            it.id = id
            DisplayUnit du = display.get(id)
            //if it doesn't exist, add it
            if(du) {
                //it does exist and we need to yank it from the existing history
                int lastRev = du.revision
                receivedUnits.get(lastRev).remove(id)
            }
            display.put(id, it)
            revisionIds.put(id, it)
        }
        println "adding ${revisionIds.size()} at revision $newRevision"
        receivedUnits.put(newRevision, revisionIds)
        revision = newRevision
    }
}
