package com.liferay.message.boards.internal.model.listener;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.view.count.model.ViewCountEntry;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = ModelListener.class)
public class ViewCountEntryModelListener
	extends BaseModelListener<ViewCountEntry> {


	@Override
	public void onAfterUpdate(ViewCountEntry viewCountEntry)
		throws ModelListenerException {


		Indexer<ViewCountEntry> viewCountIndexer = IndexerRegistryUtil.nullSafeGetIndexer(
			ViewCountEntry.class);

		try {
			viewCountIndexer.reindex(viewCountEntry);
		}
		catch (SearchException e) {
			throw new RuntimeException(e);
		}

	}
}
