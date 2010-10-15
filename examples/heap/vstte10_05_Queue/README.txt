The fifth challenge from the Verified Software Competition (VSComp) at VSTTE'10, organised by Peter Mueller and Natarajan Shankar: 

An applicative queue with a good amortized complexity can be implemented using a linked list. The queue is implemented as a record with two fields: front and rear which are linked lists so that the Front operation returns the first element in the list front and Tail returns a new queue with front as the tail of the original front list. The Enqueue operation returns a new queue by inserting an element at the head of the list rear. You have to show that the implementation maintains the invariant that queue.rear.length <= queue.front.length. You also have to show that a client invoking these operations observes an abstract queue given by a sequence. 

[Work in progress]

Not yet verified:
- AmortizedQueue's second constructor
- AmortizedQueue::enqueue
- AmortizedQueue::tail
- LinkedList::concat
- LinkedList::reverse