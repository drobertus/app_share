package com.mechzombie.appshare.state

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
class ScreenState {

    int revision

    Map<WindowParamEnum, WindowParameter> screenParams = new HashMap<WindowParamEnum, WindowParameter>()

    Map<Integer, Window> windows = new HashMap<Integer, Window>()

    void updateScreenParam(WindowParamEnum windowParamEnum, int value, int revisionNum) {
        //update the revision
        if(revision < revisionNum) {
            revision = revisionNum
        }
        WindowParameter wp = new WindowParameter(type: windowParamEnum, value: value, revision: revisionNum)
        screenParams.put(windowParamEnum, wp)
    }
}
