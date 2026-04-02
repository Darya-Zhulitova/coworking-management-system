'use client';

import Link from 'next/link';
import Button from 'react-bootstrap/Button';
import Card from 'react-bootstrap/Card';
import CardBody from 'react-bootstrap/CardBody';
import CardText from 'react-bootstrap/CardText';
import CardTitle from 'react-bootstrap/CardTitle';
import Container from 'react-bootstrap/Container';
import Stack from 'react-bootstrap/Stack';

interface HomePageClientProps {
    apiBaseUrl: string;
}

export function HomePageClient({ apiBaseUrl }: HomePageClientProps) {
    return (
        <main className="page-shell d-flex align-items-center">
            <Container className="py-5" style={{ maxWidth: 760 }}>
                <Card className="content-card">
                    <CardBody className="p-4 p-lg-5">
                        <Stack gap={3}>
                            <div>
                                <CardTitle as="h1" className="mb-2">
                                    Coworking Admin
                                </CardTitle>
                                <CardText className="text-body-secondary mb-0">
                                    API base URL: <code>{apiBaseUrl}</code>
                                </CardText>
                            </div>

                            <div>
                                <Button as={Link} href="/login">
                                    Login
                                </Button>
                            </div>
                        </Stack>
                    </CardBody>
                </Card>
            </Container>
        </main>
    );
}