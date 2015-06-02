package com.mechzombie.appshare.update

import com.mechzombie.appshare.state.DisplayUnit
import groovy.transform.CompileStatic

@CompileStatic
class WindowDelta {

    int id
    Map<String, Integer> windowParams = [:]
    List<DisplayUnit> updates = new ArrayList<DisplayUnit>()
}
