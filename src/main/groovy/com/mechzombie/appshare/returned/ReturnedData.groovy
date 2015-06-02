package com.mechzombie.appshare.returned

import com.mechzombie.appshare.state.WindowParameter
import groovy.transform.CompileStatic

@CompileStatic
class ReturnedData {

    int revision

    List<WindowParameter> screenParams = new ArrayList<WindowParameter>()
    List<WindowUpdate> windowData = new ArrayList<WindowUpdate>()

}
