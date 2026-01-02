package com.am.common.investment.service.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.am.common.investment.persistence.document.BaseDocument;
import com.am.common.investment.service.DocumentVersionService;

/**
 * Listener that automatically increments document versions before saving
 */
@Component
public class DocumentVersionListener extends AbstractMongoEventListener<Object> {
    
    private final DocumentVersionService versionService;
    
    public DocumentVersionListener(DocumentVersionService versionService) {
        this.versionService = versionService;
    }
    
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        if (source instanceof BaseDocument && !BaseDocument.class.equals(source.getClass())) {
            BaseDocument document = (BaseDocument) source;
            versionService.incrementVersion(document);
        }
    }
}
