package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum WishlistItemSagaState implements SagaState {
    DELETE_WISHLISTITEM {
        @Override
        public String getStateName() {
            return "DELETE_WISHLISTITEM";
        }
    },
    READ_WISHLISTITEM {
        @Override
        public String getStateName() {
            return "READ_WISHLISTITEM";
        }
    },
    UPDATE_WISHLISTITEM {
        @Override
        public String getStateName() {
            return "UPDATE_WISHLISTITEM";
        }
    }
}