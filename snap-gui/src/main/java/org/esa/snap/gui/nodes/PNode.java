/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.snap.gui.nodes;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.snap.gui.SnapApp;
import org.esa.snap.gui.actions.file.CloseProductAction;
import org.openide.awt.UndoRedo;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.WeakListeners;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * A node that represents a {@link org.esa.beam.framework.datamodel.Product} (=P).
 * Every {@code PNode} holds a dedicated undo/redo context.
 *
 * @author Norman
 */
class PNode extends PNNode<Product> implements PreferenceChangeListener {

    private final PContent group;

    public PNode(Product product) {
        this(product, new PContent());
    }

    private PNode(Product product, PContent group) {
        super(product, group);
        this.group = group;
        group.node = this;
        setDisplayName(product.getName());
        setShortDescription(product.getDescription());
        setIconBaseWithExtension("org/esa/snap/gui/icons/RsProduct16.gif");
        Preferences preferences = SnapApp.getDefault().getPreferences();
        preferences.addPreferenceChangeListener(WeakListeners.create(PreferenceChangeListener.class, this, preferences));
    }

    public Product getProduct() {
        return getProductNode();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return SnapApp.getDefault().getUndoManager(getProduct());
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        new CloseProductAction(Arrays.asList(getProduct())).execute();
    }

    @Override
    public Action[] getActions(boolean context) {
        return PNNodeSupport.getContextActions(getProductNode());
    }

    @Override
    public Action getPreferredAction() {
        //Define the action that will be invoked
        //when the user double-clicks on the node:
        return super.getPreferredAction();
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        String key = evt.getKey();
        if (GroupByNodeTypeAction.PREFERENCE_KEY.equals(key)) {
            group.refresh();
        }
    }

    @Override
    public PropertySet[] getPropertySets() {

        Sheet.Set set = new Sheet.Set();
        set.setDisplayName("Product Properties");
        set.put(new PropertySupport.ReadOnly<File>("fileLocation", File.class, "File", "File location") {
            @Override
            public File getValue() {
                return getProduct().getFileLocation();
            }
        });

        return Stream.concat(Stream.of(super.getPropertySets()), Stream.of(set)).toArray(PropertySet[]::new);
    }

    private boolean isGroupByNodeType() {
        return SnapApp.getDefault().getPreferences().getBoolean(GroupByNodeTypeAction.PREFERENCE_KEY, true);
    }

    /*
    @Override
    public NewType[] getNewTypes() {
        return new NewType[] {
                new NewType() {
                    @Override
                    public String getName() {
                        return "Calculated Band";
                    }

                    @Override
                    public void create() throws IOException {
                    }
                },
                new NewType() {
                    @Override
                    public String getName() {
                        return "Filtered Band";
                    }

                    @Override
                    public void create() throws IOException {
                    }
                }
        };
    }
    */

    /**
     * A child factory for nodes below a {@link PNode} that holds a {@link org.esa.beam.framework.datamodel.Product}.
     *
     * @author Norman
     */
    static class PContent extends PNGroupBase<Object> {

        PNode node;

        @Override
        protected boolean createKeys(List<Object> list) {
            Product product = node.getProduct();
            ProductNodeGroup<MetadataElement> metadataElementGroup = product.getMetadataRoot().getElementGroup();
            if (node.isGroupByNodeType()) {
                if (metadataElementGroup != null) {
                    list.addAll(Arrays.asList(metadataElementGroup.toArray()));
                }
                list.addAll(Arrays.asList(product.getIndexCodingGroup().toArray()));
                list.addAll(Arrays.asList(product.getFlagCodingGroup().toArray()));
                list.addAll(Arrays.asList(product.getVectorDataGroup().toArray()));
                list.addAll(Arrays.asList(product.getTiePointGridGroup().toArray()));
                list.addAll(Arrays.asList(product.getBandGroup().toArray()));
                list.addAll(Arrays.asList(product.getMaskGroup().toArray()));
            } else {
                if (metadataElementGroup != null) {
                    list.add(new PNGGroup.ME(metadataElementGroup));
                }
                if (product.getIndexCodingGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.IC(product.getIndexCodingGroup()));
                }
                if (product.getFlagCodingGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.FC(product.getFlagCodingGroup()));
                }
                if (product.getVectorDataGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.VDN(product.getVectorDataGroup()));
                }
                if (product.getTiePointGridGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.TPG(product.getTiePointGridGroup()));
                }
                if (product.getBandGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.B(product.getBandGroup()));
                }
                if (product.getMaskGroup().getNodeCount() > 0) {
                    list.add(new PNGGroup.M(product.getMaskGroup()));
                }
            }

            return true;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (key instanceof ProductNode) {
                return PNNode.create((ProductNode) key);
            } else {
                return new PNGroupNode((PNGGroup) key);
            }
        }
    }
}
