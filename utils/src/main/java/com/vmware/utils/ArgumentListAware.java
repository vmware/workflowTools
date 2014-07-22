package com.vmware.utils;

import jline.console.completer.ArgumentCompleter;

public interface ArgumentListAware {

    public void setArgumentList(ArgumentCompleter.ArgumentList argumentList);
}
