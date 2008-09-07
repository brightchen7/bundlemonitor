/*
 * Copyright (c) 2008 Neil Bartlett
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial implementation
 */
package name.neilbartlett.eclipse.bundlemonitor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

public class BundlesView extends ViewPart {

	private TableViewer tableViewer;
	private BundlesContentProvider contentProvider;

	private IAction propertiesAction;
	private BundleSymbolicNameFilter nameFilter;

	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2,false));
		
		new Label(composite, SWT.NONE).setText("Filter:");
		final Text txtFilter = new Text(composite, SWT.BORDER);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		
		Table table = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION
				| SWT.MULTI);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		TableColumn col = new TableColumn(table, SWT.BORDER);
		col.setWidth(40);
		col.setText("Id");

		col = new TableColumn(table, SWT.NONE);
		col.setWidth(100);
		col.setText("State");

		col = new TableColumn(table, SWT.NONE);
		col.setWidth(250);
		col.setText("Name");
		
		col = new TableColumn(table, SWT.NONE);
		col.setWidth(250);
		col.setText("Location");

		tableViewer = new TableViewer(table);
		contentProvider = new BundlesContentProvider(tableViewer, Activator
				.getDefault().getBundleContext());
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setSorter(new BundleIdSorter());
		tableViewer.setLabelProvider(new BundleTableLabelProvider());
		
		nameFilter = new BundleSymbolicNameFilter();
		tableViewer.addFilter(nameFilter);
		txtFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				nameFilter.setFilterString(txtFilter.getText());
				tableViewer.refresh();
			}
		});
		
		tableViewer.setInput(Activator.getDefault().getBundleContext());

		contentProvider.start();
		getViewSite().setSelectionProvider(tableViewer);

		createActions();
		fillMenu();

		initContextMenu();
	}

	private void createActions() {
		propertiesAction = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) tableViewer
						.getSelection();
				if (!selection.isEmpty()) {
					Bundle bundle = (Bundle) selection.getFirstElement();

					BundlePropertiesDialog propsDialog = new BundlePropertiesDialog(
							getSite().getShell(), bundle);
					propsDialog.open();
				}
			}
		};
		propertiesAction.setText("Properties...");
	}

	protected void fillMenu() {
		IMenuManager menuManager = getViewSite().getActionBars()
				.getMenuManager();
		menuManager.add(new ExcludeBundlesFilterAction("Installed",
				Bundle.INSTALLED, tableViewer));
		menuManager.add(new ExcludeBundlesFilterAction("Resolved",
				Bundle.RESOLVED, tableViewer));
		menuManager.add(new ExcludeBundlesFilterAction("Starting",
				Bundle.STARTING, tableViewer));
		menuManager.add(new ExcludeBundlesFilterAction("Active", Bundle.ACTIVE,
				tableViewer));
		menuManager.add(new ExcludeBundlesFilterAction("Stopping",
				Bundle.STOPPING, tableViewer));
	}

	protected void fillToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars()
				.getToolBarManager();

		MenuManager filterMenu = new MenuManager();
		toolBarManager.add(filterMenu);
	}

	protected void initContextMenu() {
		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				menuMgr.add(propertiesAction);
				menuMgr.add(new Separator());
				menuMgr.add(new Separator(
						IWorkbenchActionConstants.MB_ADDITIONS));
			}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tableViewer);
	}

	public void setFocus() {
	}

	public void dispose() {
		contentProvider.stop();
	}

}
