package com.mechzombie.appshare.returned

import com.mechzombie.appshare.state.WindowParamEnum
import com.mechzombie.appshare.state.DisplayUnit
import groovy.transform.CompileStatic

@CompileStatic
class WindowUpdate {

    int id
    Map<WindowParamEnum, Integer> params = new HashMap<WindowParamEnum, Integer>()
    List<DisplayUnit> contents = new ArrayList<DisplayUnit>()

}
