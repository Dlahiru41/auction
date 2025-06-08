package com.auction.session;

import com.auction.entity.User;
import jakarta.json.Json;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

@Stateless
public class UserSessionBean implements UserSessionBeanLocal, UserSessionBeanRemote {

    private static final Logger logger = Logger.getLogger(UserSessionBean.class.getName());

    @PersistenceContext(unitName = "auctionPU")
    private EntityManager em;

    @Override
    public User createUser(String email, String password, String firstName, String lastName) {
        try {
            // Check if user already exists
            User existingUser = findUserByEmail(email);
            if (existingUser != null) {
                throw new IllegalArgumentException("User with email " + email + " already exists");
            }

            User user = new User(email, password, firstName, lastName);
            em.persist(user);
            em.flush();

            logger.info("Created new user: " + email);
            return user;
        } catch (Exception e) {
            logger.severe("Error retrieving system status: " + e.getMessage());
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity(Json.createObjectBuilder()
//                            .add("success", false)
//                            .add("error", "Failed to retrieve system status")
//                            .build())
//                    .build();
            return null;
        }
    }
    @Override
    public User findUserByEmail(String email) {
        try {
            Query query = em.createNamedQuery("User.findByEmail");
            query.setParameter("email", email);
            return (User) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public User authenticateUser(String email, String password) {
        User user = findUserByEmail(email);
        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            return user;
        }
        return null;
    }

    @Override
    public List<User> findActiveUsers() {
        Query query = em.createNamedQuery("User.findActiveUsers");
        return query.getResultList();
    }

    @Override
    public User updateUser(User user) {
        return em.merge(user);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = em.find(User.class, userId);
        if (user != null) {
            user.setActive(false);
            em.merge(user);
        }
    }

    @Override
    public User findUserById(Long userId) {
        return em.find(User.class, userId);
    }
}