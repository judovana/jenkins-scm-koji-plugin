package org.fakekoji.jobmanager.manager;

import org.fakekoji.jobmanager.ManagementException;
import org.fakekoji.jobmanager.Manager;
import org.fakekoji.model.Product;
import org.fakekoji.storage.Storage;
import org.fakekoji.storage.StorageException;

import java.util.List;

public class ProductManager implements Manager<Product> {

    private final Storage<Product> storage;

    public ProductManager(final Storage<Product> storage) {
        this.storage = storage;
    }

    @Override
    public void create(Product product) throws StorageException, ManagementException {
        if (storage.contains(product.getId())) {
            throw new ManagementException("Product with id " + product.getId() + " already exists");
        }
        storage.store(product.getId(), product);
    }

    @Override
    public Product read(String id) throws StorageException, ManagementException {
        if (!storage.contains(id)) {
            throw new ManagementException("No product with id: " + id);
        }
        return storage.load(id, Product.class);
    }

    @Override
    public List<Product> readAll() throws StorageException {
        return storage.loadAll(Product.class);
    }

    @Override
    public void update(String id, Product product) throws StorageException, ManagementException {
        return;
    }

    @Override
    public void delete(String id) throws StorageException, ManagementException {
        return;
    }
}
