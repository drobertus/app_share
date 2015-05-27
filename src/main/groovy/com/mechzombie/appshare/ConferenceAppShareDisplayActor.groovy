package com.mechzombie.appshare

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.actor.DefaultActor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList

@CompileStatic
@Slf4j
class ConferenceAppShareDisplayActor extends DefaultActor {

    /**
     * Screen has a full map of the current display
     */
    Map<String, DisplayUnit> screen

    /**
     * received units maintains an ordered list of messages.
     * As new units are received existing ones earlier in the list are removed
     *
     */
    ConcurrentSkipListMap<Long, Map<String, DisplayUnit>> receivedUnits =
        new ConcurrentSkipListMap<Long, Map<String, DisplayUnit>>()


    ConferenceAppShareDisplayActor(int height, int width) {
        screen = new ConcurrentHashMap<String, DisplayUnit>((height * width) + 20)
    }

    @Override
    protected void act() {
        loop {
            react { List<DisplayUnit> inboundChanges ->
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
    private def addUpdates(List<DisplayUnit> changes) {
        long arrivalTime = System.currentTimeMillis()

        Map<String, DisplayUnit> newMap = new HashMap<String, DisplayUnit>()
        receivedUnits.put(arrivalTime, newMap)
        changes.each {
            it.id = "${it.x}-${it.y}"
            it.arrivalTime = arrivalTime

            newMap.put(it.id, it)

            DisplayUnit lastAddedUnit = screen.get(it.id)
            screen.put(it.id, it)

            //get the data of when it was last inserted
            if(lastAddedUnit) {
                //remove it from the display
                //the assumption is we can make this a simple map since it is only modified at the
                //level of the add, by one thread
                Map<String, DisplayUnit> areaCandidate = receivedUnits.get(lastAddedUnit.arrivalTime)
                DisplayUnit du = areaCandidate.remove(it.id)
                if(du) {
                    println "removing an element ${it.id} from time ${lastAddedUnit.arrivalTime}"
                }
            }
        }
    }


    ReturnedData getData(Long time = null) {
        if(time) {
            return getChangesSinceTime(time)
        }
        return getCurrentState()
    }

    private ReturnedData getCurrentState() {
        ReturnedData rd = new ReturnedData()
        rd.returnTime = System.currentTimeMillis()
        rd.returnedData.addAll(screen.values())
        log.info "current state returned at ${rd.returnTime}"
        return rd
    }

    private ReturnedData getChangesSinceTime(long time) {

        println ("getting changes since $time")
        ReturnedData rd = new ReturnedData()
        rd.returnTime = System.currentTimeMillis()
        List<DisplayUnit> theResults = rd.returnedData
        NavigableSet<Long> keys = receivedUnits.descendingKeySet()
        Iterator<Long> iterator = keys.iterator()
        while(iterator.hasNext()) {
            long installedTime = iterator.next()

            if(installedTime <= time) {
                break;
            }
            theResults.addAll(receivedUnits.get(installedTime).values())
        }
        println "returning delta of size = ${theResults.size()}"
        return rd
    }
}
