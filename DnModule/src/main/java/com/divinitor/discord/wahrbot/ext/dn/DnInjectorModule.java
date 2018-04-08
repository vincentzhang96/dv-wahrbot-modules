package com.divinitor.discord.wahrbot.ext.dn;

import com.divinitor.discord.wahrbot.ext.dn.services.DnStatService;
import com.divinitor.discord.wahrbot.ext.dn.services.impl.HardcodedDnStatService;
import com.divinitor.discord.wahrbot.ext.dn.util.QueueExceptionHandler;
import com.google.inject.Binder;
import com.google.inject.Module;

public class DnInjectorModule implements Module {

    private final DnModule module;

    public DnInjectorModule(DnModule module) {
        this.module = module;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(DnStatService.class).toInstance(new HardcodedDnStatService());
        binder.bind(DnModule.Accessor.class).toInstance(this.module.new Accessor());
        binder.bind(QueueExceptionHandler.class).toProvider(this.module::getExceptionHandler);
    }
}
