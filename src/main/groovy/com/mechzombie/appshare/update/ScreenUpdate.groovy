package com.mechzombie.appshare.update

import groovy.transform.CompileStatic

@CompileStatic
class ScreenUpdate {

    Integer screenHeight
    Integer screenWidth
    List<WindowDelta> windowDeltas = new ArrayList<WindowDelta>()
}
