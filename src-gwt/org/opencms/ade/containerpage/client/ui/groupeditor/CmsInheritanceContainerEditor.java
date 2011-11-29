/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.containerpage.client.ui.groupeditor;

import org.opencms.ade.containerpage.client.CmsContainerpageController;
import org.opencms.ade.containerpage.client.CmsContainerpageHandler;
import org.opencms.ade.containerpage.client.CmsContainerpageUtil;
import org.opencms.ade.containerpage.client.Messages;
import org.opencms.ade.containerpage.client.ui.CmsContainerPageElementPanel;
import org.opencms.ade.containerpage.client.ui.CmsElementOptionBar;
import org.opencms.ade.containerpage.client.ui.CmsGroupContainerElementPanel;
import org.opencms.ade.containerpage.client.ui.css.I_CmsLayoutBundle;
import org.opencms.ade.containerpage.shared.CmsContainerElement;
import org.opencms.ade.containerpage.shared.CmsContainerElementData;
import org.opencms.ade.containerpage.shared.CmsInheritanceContainer;
import org.opencms.ade.containerpage.shared.CmsInheritanceInfo;
import org.opencms.gwt.client.ui.CmsPushButton;
import org.opencms.gwt.client.ui.CmsToggleButton;
import org.opencms.gwt.client.ui.I_CmsButton.ButtonColor;
import org.opencms.gwt.client.ui.I_CmsButton.ButtonStyle;
import org.opencms.gwt.client.ui.input.CmsTextBox;
import org.opencms.gwt.client.util.CmsDebugLog;
import org.opencms.gwt.client.util.CmsDomUtil;
import org.opencms.gwt.client.util.CmsDomUtil.Tag;
import org.opencms.gwt.client.util.I_CmsSimpleCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The inheritance container editor.<p>
 * 
 * @since 8.5.0
 */
public class CmsInheritanceContainerEditor extends A_CmsGroupEditor {

    /** Css class to mark hidden elements. */
    private static final String HIDDEN_ELEMENT_CLASS = I_CmsLayoutBundle.INSTANCE.containerpageCss().hiddenElement();

    /** Css class to hide elements. */
    private static final String HIDE_ELEMENTS_CLASS = I_CmsLayoutBundle.INSTANCE.containerpageCss().hideElements();

    /** Css class to hide elements. */
    private static final String HIDDEN_ELEMENT_OVERLAY_CLASS = I_CmsLayoutBundle.INSTANCE.containerpageCss().hiddenElementOverlay();

    /** The editor instance. */
    private static CmsInheritanceContainerEditor INSTANCE;

    /** The description input. */
    private CmsTextBox m_inputDescription;

    /** The inheritance line name input. */
    private CmsTextBox m_inputName;

    /** The title input. */
    private CmsTextBox m_inputTitle;

    /** Click handler for the option buttons. */
    private ClickHandler m_optionClickHandler;

    /**
     * Constructor.<p>
     * 
     * @param groupContainer the group container widget
     * @param controller the container page controller
     * @param handler the container page handler
     */
    protected CmsInheritanceContainerEditor(
        CmsGroupContainerElementPanel groupContainer,
        CmsContainerpageController controller,
        CmsContainerpageHandler handler) {

        super(groupContainer, controller, handler);
        m_optionClickHandler = new ClickHandler() {

            public void onClick(ClickEvent event) {

                I_CmsGroupEditorOption optionButton = (I_CmsGroupEditorOption)event.getSource();
                optionButton.onClick(event);
            }
        };
        // Loading data of all contained elements including inherit container element
        getController().getElements(getElementIds(), new I_CmsSimpleCallback<Map<String, CmsContainerElementData>>() {

            public void execute(Map<String, CmsContainerElementData> arg) {

                setInheritContainerData(arg);
            }
        });
        getGroupContainerWidget().addStyleName(HIDE_ELEMENTS_CLASS);
    }

    /**
     * Opens the inheritance container editor.<p>
     * 
     * @param groupContainer the group-container
     * @param controller the container-page controller
     * @param handler the container-page handler
     */
    public static void openInheritanceContainerEditor(
        CmsGroupContainerElementPanel groupContainer,
        CmsContainerpageController controller,
        CmsContainerpageHandler handler) {

        // making sure only a single instance of the group-container editor is open
        if (INSTANCE != null) {
            CmsDebugLog.getInstance().printLine("group-container editor already open");
        } else {
            if (controller.startEditingGroupcontainer(groupContainer)) {
                INSTANCE = new CmsInheritanceContainerEditor(groupContainer, controller, handler);
                RootPanel.get().add(INSTANCE);
                INSTANCE.openDialog(Messages.get().key(Messages.GUI_INHERITANCECONTAINER_CAPTION_0));
                INSTANCE.getGroupContainerWidget().refreshHighlighting();
            }
        }
    }

    /**
     * Either removes the locally configured element or hides the inherited element.<p>
     * 
     * @param elementWidget the element widget
     */
    public void removeElement(CmsContainerPageElementPanel elementWidget) {

        if (elementWidget.getInheritanceInfo().isNew()) {
            getHandler().removeElement(elementWidget);
        } else {
            elementWidget.getInheritanceInfo().setVisible(false);
            elementWidget.addStyleName(HIDDEN_ELEMENT_CLASS);
            Element elementOverlay = DOM.createDiv();
            elementOverlay.setClassName(HIDDEN_ELEMENT_OVERLAY_CLASS);
            elementWidget.getElement().appendChild(elementOverlay);
            getGroupContainerWidget().add(elementWidget);
            updateButtonVisibility(elementWidget);
        }
        getGroupContainerWidget().refreshHighlighting();
    }

    /**
     * Shows a formerly hidden element and sets the visibility info to true.<p>
     * 
     * @param elementWidget the element widget
     */
    public void showElement(CmsContainerPageElementPanel elementWidget) {

        int index = 0;
        for (index = 0; index < getGroupContainerWidget().getWidgetCount(); index++) {
            if (CmsDomUtil.hasClass(HIDDEN_ELEMENT_CLASS, getGroupContainerWidget().getWidget(index).getElement())) {
                break;
            }
        }
        getGroupContainerWidget().insert(elementWidget, index);
        elementWidget.getInheritanceInfo().setVisible(true);
        elementWidget.removeStyleName(HIDDEN_ELEMENT_CLASS);
        updateButtonVisibility(elementWidget);
        getGroupContainerWidget().refreshHighlighting();
        List<Element> elements = CmsDomUtil.getElementsByClass(
            HIDDEN_ELEMENT_OVERLAY_CLASS,
            Tag.div,
            elementWidget.getElement());
        for (Element element : elements) {
            element.removeFromParent();
        }
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#addButtons()
     */
    @Override
    protected void addButtons() {

        addCancelButton();
        addSaveButton();
        CmsToggleButton showElementsButton = new CmsToggleButton();
        showElementsButton.setText(Messages.get().key(Messages.GUI_INHERITANCECONTAINER_SHOW_HIDDEN_0));
        showElementsButton.setDownFace(Messages.get().key(Messages.GUI_INHERITANCECONTAINER_HIDE_ELEMENTS_0), null);
        showElementsButton.setUseMinWidth(true);
        showElementsButton.setButtonStyle(ButtonStyle.TEXT, ButtonColor.RED);
        showElementsButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                toggleElementVisibility();
            }
        });
        addButton(showElementsButton);
        CmsPushButton breakUpButton = new CmsPushButton();
        breakUpButton.setText(Messages.get().key(Messages.GUI_BUTTON_BREAK_UP_TEXT_0));
        breakUpButton.setUseMinWidth(true);
        breakUpButton.setButtonStyle(ButtonStyle.TEXT, ButtonColor.RED);
        breakUpButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {

                breakUpContainer();
            }
        });
        addButton(breakUpButton);
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#addInputFields()
     */
    @Override
    protected void addInputFields() {

        m_inputTitle = new CmsTextBox();
        addInputField(Messages.get().key(Messages.GUI_GROUPCONTAINER_LABEL_TITLE_0), m_inputTitle);
        m_inputDescription = new CmsTextBox();
        addInputField(Messages.get().key(Messages.GUI_GROUPCONTAINER_LABEL_DESCRIPTION_0), m_inputDescription);
        m_inputName = new CmsTextBox();
        addInputField(Messages.get().key(Messages.GUI_INHERITANCECONTAINER_CONFIG_NAME_0), m_inputName);
    }

    /**
     * Breaks up the group container inserting all visible elements into the parent container instead.<p>
     */
    protected void breakUpContainer() {

        List<String> clientIds = new ArrayList<String>();
        for (Widget w : getGroupContainerWidget()) {
            if (w instanceof CmsContainerPageElementPanel) {
                CmsContainerPageElementPanel elementWidget = (CmsContainerPageElementPanel)w;
                if ((elementWidget.getInheritanceInfo() == null) || elementWidget.getInheritanceInfo().isVisibile()) {
                    clientIds.add(elementWidget.getId());
                }
            }
        }
        int index = getIndexPosition();
        for (String clientId : clientIds) {
            try {
                CmsContainerElementData elementData = getController().getCachedElement(clientId);
                CmsContainerPageElementPanel containerElement = getController().getContainerpageUtil().createElement(
                    elementData,
                    getParentContainer());
                getParentContainer().insert(containerElement, index);
                index++;
            } catch (Exception e) {
                CmsDebugLog.getInstance().printLine(e.getMessage());
            }
        }
        getController().addToRecentList(getGroupContainerWidget().getId());
        getController().setPageChanged();
        closeDialog(true);
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#cancelEdit()
     */
    @Override
    protected void cancelEdit() {

        removeAllChildren();
        for (CmsContainerPageElementPanel element : getBackUpElements()) {
            getGroupContainerWidget().add(element);
        }
        if (getBackUpElements().size() == 0) {
            getGroupContainerWidget().addStyleName(I_CmsLayoutBundle.INSTANCE.containerpageCss().emptyGroupContainer());
        }
        closeDialog(false);
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#clearInstance()
     */
    @Override
    protected void clearInstance() {

        INSTANCE = null;
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#closeDialog(boolean)
     */
    @Override
    protected void closeDialog(boolean breakingUp) {

        getGroupContainerWidget().removeStyleName(I_CmsLayoutBundle.INSTANCE.containerpageCss().hideElements());
        super.closeDialog(breakingUp);
    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.groupeditor.A_CmsGroupEditor#saveEdit()
     */
    @Override
    protected void saveEdit() {

        List<CmsContainerElement> elements = new ArrayList<CmsContainerElement>();
        for (Widget widget : getGroupContainerWidget()) {
            if (widget instanceof CmsContainerPageElementPanel) {
                CmsContainerPageElementPanel elementWidget = (CmsContainerPageElementPanel)widget;
                CmsInheritanceInfo inheritanceInfo = elementWidget.getInheritanceInfo();
                if (inheritanceInfo == null) {
                    inheritanceInfo = new CmsInheritanceInfo(null, true, true);
                }
                CmsContainerElement element = getController().getCachedElement(elementWidget.getId());
                element.setInheritanceInfo(inheritanceInfo);
                elements.add(element);
            }
        }
        CmsInheritanceContainer container = new CmsInheritanceContainer();
        container.setNew(getGroupContainerWidget().isNew());
        container.setClientId(getGroupContainerWidget().getId());
        container.setTitle(m_inputTitle.getText());
        container.setDescription(m_inputDescription.getText());
        container.setName(m_inputName.getText());
        container.setElements(elements);
        getController().saveInheritContainer(container, getGroupContainerWidget());
        closeDialog(false);
    }

    /**
     * Sets the loaded element data.<p>
     * 
     * @param elementsData the elements data
     */
    protected void setInheritContainerData(Map<String, CmsContainerElementData> elementsData) {

        m_elementData = elementsData.get(getGroupContainerWidget().getId());
        if (m_elementData != null) {
            m_inputDescription.setFormValueAsString(m_elementData.getDescription());
            m_inputTitle.setFormValueAsString(m_elementData.getTitle());
            m_inputName.setFormValueAsString(m_elementData.getInheritanceName());
            // prevent editing the inheritance name
            m_inputName.setEnabled(false);
            removeAllChildren();
            CmsContainerpageUtil util = getController().getContainerpageUtil();
            for (CmsInheritanceInfo info : m_elementData.getInheritanceInfos()) {
                if (info.isVisibile()) {
                    CmsContainerElementData element = getController().getCachedElement(info.getClientId());
                    try {
                        CmsContainerPageElementPanel elementWidget = util.createElement(
                            element,
                            getGroupContainerWidget());
                        elementWidget.setInheritanceInfo(info);
                        elementWidget.setElementOptionBar(createOptionBar(elementWidget));
                        getGroupContainerWidget().add(elementWidget);
                        updateButtonVisibility(elementWidget);
                    } catch (Exception e) {
                        CmsDebugLog.getInstance().printLine(e.getMessage());
                    }
                }
            }
            for (CmsInheritanceInfo info : m_elementData.getInheritanceInfos()) {
                if (!info.isVisibile()) {
                    CmsContainerElementData element = getController().getCachedElement(info.getClientId());
                    try {
                        CmsContainerPageElementPanel elementWidget = util.createElement(
                            element,
                            getGroupContainerWidget());
                        elementWidget.setInheritanceInfo(info);
                        elementWidget.addStyleName(HIDDEN_ELEMENT_CLASS);
                        elementWidget.setElementOptionBar(createOptionBar(elementWidget));
                        getGroupContainerWidget().add(elementWidget);
                        updateButtonVisibility(elementWidget);
                        Element elementOverlay = DOM.createDiv();
                        elementOverlay.setClassName(HIDDEN_ELEMENT_OVERLAY_CLASS);
                        elementWidget.getElement().appendChild(elementOverlay);
                    } catch (Exception e) {
                        CmsDebugLog.getInstance().printLine(e.getMessage());
                    }
                }
            }
        }
        getGroupContainerWidget().refreshHighlighting();
        this.setSaveEnabled(true, null);
    }

    /**
     * Toggles the visibility of hidden elements.<p>
     */
    protected void toggleElementVisibility() {

        if (CmsDomUtil.hasClass(HIDE_ELEMENTS_CLASS, getGroupContainerWidget().getElement())) {
            getGroupContainerWidget().removeStyleName(HIDE_ELEMENTS_CLASS);
        } else {
            getGroupContainerWidget().addStyleName(HIDE_ELEMENTS_CLASS);
        }
        getGroupContainerWidget().refreshHighlighting();
    }

    /**
     * Creates an option bar for the given element.<p>
     * 
     * @param elementWidget the element widget
     * 
     * @return the option bar
     */
    private CmsElementOptionBar createOptionBar(CmsContainerPageElementPanel elementWidget) {

        CmsElementOptionBar optionBar = new CmsElementOptionBar(elementWidget);
        CmsPushButton button = new CmsFavoritesOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsSettingsOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsRemoveOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsEditOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsMoveOptionButton(elementWidget, this);
        // setting the drag and drop handler
        button.addMouseDownHandler(getController().getDndHandler());
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsAddOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsInheritedOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);
        button = new CmsSelectionOptionButton(elementWidget, this);
        button.addClickHandler(m_optionClickHandler);
        optionBar.add(button);

        return optionBar;
    }

    /**
     * Updates the visibility of the option bar buttons of the given element.<p>
     * 
     * @param elementWidget the element widget
     */
    private void updateButtonVisibility(CmsContainerPageElementPanel elementWidget) {

        Iterator<Widget> it = elementWidget.getElementOptionBar().iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof I_CmsGroupEditorOption) {
                if (((I_CmsGroupEditorOption)w).checkVisibility()) {
                    w.getElement().getStyle().clearDisplay();
                } else {
                    w.getElement().getStyle().setDisplay(Display.NONE);
                }
            }
        }
        elementWidget.getElementOptionBar().initWidth();
    }
}